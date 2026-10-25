package com.project.ridemesh.booking.controllers;

import com.project.ridemesh.booking.dto.*;
import com.project.ridemesh.booking.entities.Booking;
import com.project.ridemesh.booking.exceptions.BookingNotFoundException;
import com.project.ridemesh.booking.exceptions.InvalidBookingStateException;
import com.project.ridemesh.booking.services.BookingService;
import com.project.ridemesh.dto.UserDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 1. POST /api/bookings:
     * Customer creates a new service booking request. Computes 95/5 financial split and sets status = REQUESTED.
     */
    @PostMapping
    public ResponseEntity<BookingApiResponse<Booking>> createBooking(@Valid @RequestBody CreateBookingDto dto) {
        // If customerId is not specified, attempt to retrieve from SecurityContext
        if (dto.getCustomerId() == null) {
            Long authUserId = getAuthenticatedUserId();
            if (authUserId != null) {
                dto.setCustomerId(authUserId);
            }
        }

        Booking booking = bookingService.createBooking(dto);
        return new ResponseEntity<>(BookingApiResponse.success(booking, "Booking request created successfully"), HttpStatus.CREATED);
    }

    /**
     * 2. GET /api/bookings/worker/pending:
     * Returns unassigned bookings (status == REQUESTED AND workerId == null) + direct bookings (status == REQUESTED AND workerId == currentWorkerId).
     */
    @GetMapping("/worker/pending")
    public ResponseEntity<BookingApiResponse<List<Booking>>> getWorkerPendingBookings(
            @RequestParam(required = false) Long workerId) {
        if (workerId == null) {
            workerId = getAuthenticatedUserId();
        }

        List<Booking> bookings = bookingService.getWorkerPendingBookings(workerId);
        return ResponseEntity.ok(BookingApiResponse.success(bookings, "Pending bookings retrieved successfully"));
    }

    /**
     * 3. PATCH /api/bookings/{id}/accept:
     * Worker accepts: sets workerId and status = ACCEPTED.
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<BookingApiResponse<Booking>> acceptBooking(
            @PathVariable Long id,
            @RequestBody(required = false) AcceptBookingDto dto) {
        if (dto == null) {
            dto = new AcceptBookingDto();
        }
        if (dto.getWorkerId() == null) {
            dto.setWorkerId(getAuthenticatedUserId());
        }

        Booking booking = bookingService.acceptBooking(id, dto);
        return ResponseEntity.ok(BookingApiResponse.success(booking, "Booking accepted successfully"));
    }

    /**
     * 4. PATCH /api/bookings/{id}/decline:
     * Worker declines: sets status = REJECTED.
     */
    @PatchMapping("/{id}/decline")
    public ResponseEntity<BookingApiResponse<Booking>> declineBooking(
            @PathVariable Long id,
            @RequestBody(required = false) DeclineBookingDto dto) {
        if (dto == null) {
            dto = new DeclineBookingDto();
        }
        if (dto.getWorkerId() == null) {
            dto.setWorkerId(getAuthenticatedUserId());
        }

        Booking booking = bookingService.declineBooking(id, dto);
        return ResponseEntity.ok(BookingApiResponse.success(booking, "Booking declined successfully"));
    }

    /**
     * 5. PATCH /api/bookings/{id}/start:
     * Worker begins work: sets status = IN_PROGRESS.
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<BookingApiResponse<Booking>> startBooking(
            @PathVariable Long id,
            @RequestParam(required = false) Long workerId) {
        if (workerId == null) {
            workerId = getAuthenticatedUserId();
        }

        Booking booking = bookingService.startBooking(id, workerId);
        return ResponseEntity.ok(BookingApiResponse.success(booking, "Service started on site (IN_PROGRESS)"));
    }

    /**
     * 6. PATCH /api/bookings/{id}/complete:
     * Worker completes work: sets status = COMPLETED. Triggers release of 95% worker payout and retains 5% platform fee.
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<BookingApiResponse<Booking>> completeBooking(
            @PathVariable Long id,
            @RequestParam(required = false) Long workerId) {
        if (workerId == null) {
            workerId = getAuthenticatedUserId();
        }

        Booking booking = bookingService.completeBooking(id, workerId);
        return ResponseEntity.ok(BookingApiResponse.success(booking, "Service marked as COMPLETED. Payout and platform fee released successfully"));
    }

    /**
     * 7. PATCH /api/bookings/{id}/cancel:
     * Customer cancels:
     * - Allowed only if status is REQUESTED or ACCEPTED.
     * - Sets status = CANCELLED.
     * - Releases escrow/pre-authorized payment back to customer.
     * - If status is IN_PROGRESS or COMPLETED, throws a 400 Bad Request error.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingApiResponse<Booking>> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancelBookingDto dto) {
        if (dto == null) {
            dto = new CancelBookingDto();
        }
        if (dto.getCustomerId() == null) {
            dto.setCustomerId(getAuthenticatedUserId());
        }

        Booking booking = bookingService.cancelBooking(id, dto);
        return ResponseEntity.ok(BookingApiResponse.success(booking, "Booking cancelled successfully. Escrow funds returned to customer"));
    }

    /**
     * 8. GET /api/bookings/customer:
     * Returns all bookings for the authenticated customer.
     */
    @GetMapping("/customer")
    public ResponseEntity<BookingApiResponse<List<Booking>>> getCustomerBookings(
            @RequestParam(required = false) Long customerId) {
        if (customerId == null) {
            customerId = getAuthenticatedUserId();
        }

        if (customerId == null) {
            return new ResponseEntity<>(BookingApiResponse.error("customerId must be provided as a query param or via authentication header"), HttpStatus.BAD_REQUEST);
        }

        List<Booking> bookings = bookingService.getCustomerBookings(customerId);
        return ResponseEntity.ok(BookingApiResponse.success(bookings, "Customer bookings retrieved successfully"));
    }

    /**
     * GET /api/bookings/{id}:
     * Retrieve single booking details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingApiResponse<Booking>> getBookingById(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(BookingApiResponse.success(booking, "Booking details retrieved successfully"));
    }

    // --- Error Handlers for Standard ApiResponse Envelope ---

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<BookingApiResponse<Void>> handleInvalidBookingState(InvalidBookingStateException ex) {
        log.warn("Invalid booking state transition: {}", ex.getMessage());
        return new ResponseEntity<>(BookingApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<BookingApiResponse<Void>> handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("Booking not found: {}", ex.getMessage());
        return new ResponseEntity<>(BookingApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDto userDto) {
            return userDto.getId();
        }
        return null;
    }
}
