package com.project.ridemesh.booking.services;

import com.project.ridemesh.booking.dto.AcceptBookingDto;
import com.project.ridemesh.booking.dto.CancelBookingDto;
import com.project.ridemesh.booking.dto.CreateBookingDto;
import com.project.ridemesh.booking.dto.DeclineBookingDto;
import com.project.ridemesh.booking.entities.Booking;
import com.project.ridemesh.booking.enums.BookingStatus;
import com.project.ridemesh.booking.exceptions.BookingNotFoundException;
import com.project.ridemesh.booking.exceptions.InvalidBookingStateException;
import com.project.ridemesh.booking.repositories.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public Booking createBooking(CreateBookingDto dto) {
        log.info("Creating new booking request for customer: {} (ID: {})", dto.getCustomerName(), dto.getCustomerId());

        // Cooperative 95/5 Split Calculation
        double estimatedPrice = dto.getEstimatedPrice();
        double platformFee = (double) Math.round(estimatedPrice * 0.05);
        double workerPayout = estimatedPrice - platformFee;

        Booking booking = new Booking(
                dto.getCustomerId(),
                dto.getCustomerName(),
                dto.getCustomerPhone(),
                dto.getWorkerId(),
                dto.getWorkerName(),
                dto.getServiceCategory(),
                dto.getServiceDescription(),
                dto.getLocation(),
                dto.getScheduledDate(),
                dto.getScheduledTime(),
                estimatedPrice,
                platformFee,
                workerPayout,
                BookingStatus.REQUESTED
        );

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully with ID: {}, estimatedPrice: ₹{}, platformFee: ₹{}, workerPayout: ₹{}",
                savedBooking.getId(), estimatedPrice, platformFee, workerPayout);

        return savedBooking;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getWorkerPendingBookings(Long workerId) {
        if (workerId != null) {
            log.info("Fetching pending bookings for worker ID: {} (unassigned + assigned to worker)", workerId);
            return bookingRepository.findPendingBookingsForWorker(BookingStatus.REQUESTED, workerId);
        } else {
            log.info("Fetching all unassigned pending bookings");
            return bookingRepository.findByStatusAndWorkerIdIsNullOrderByCreatedAtDesc(BookingStatus.REQUESTED);
        }
    }

    @Override
    @Transactional
    public Booking acceptBooking(Long bookingId, AcceptBookingDto dto) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new InvalidBookingStateException("Cannot accept booking in " + booking.getStatus() + " state. Must be in REQUESTED state.");
        }

        Long workerId = (dto != null && dto.getWorkerId() != null) ? dto.getWorkerId() : null;
        if (booking.getWorkerId() != null && workerId != null && !booking.getWorkerId().equals(workerId)) {
            throw new InvalidBookingStateException("Booking is assigned directly to another worker.");
        }

        if (workerId != null) {
            booking.setWorkerId(workerId);
        }
        if (dto != null && dto.getWorkerName() != null) {
            booking.setWorkerName(dto.getWorkerName());
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAcceptedAt(LocalDateTime.now());

        log.info("Worker {} accepted booking ID: {}", booking.getWorkerId(), booking.getId());
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking declineBooking(Long bookingId, DeclineBookingDto dto) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new InvalidBookingStateException("Cannot decline booking in " + booking.getStatus() + " state. Must be in REQUESTED state.");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectedAt(LocalDateTime.now());
        if (dto != null && dto.getReason() != null) {
            booking.setCancellationReason(dto.getReason());
        }

        log.info("Booking ID: {} was declined by worker {}. Reason: {}", booking.getId(), booking.getWorkerId(), booking.getCancellationReason());
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking startBooking(Long bookingId, Long workerId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidBookingStateException("Cannot start booking in " + booking.getStatus() + " state. Must be in ACCEPTED state.");
        }

        if (workerId != null && booking.getWorkerId() != null && !booking.getWorkerId().equals(workerId)) {
            throw new InvalidBookingStateException("Worker ID does not match the assigned worker for this booking.");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setStartedAt(LocalDateTime.now());

        log.info("Worker {} started service on site for booking ID: {}", booking.getWorkerId(), booking.getId());
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking completeBooking(Long bookingId, Long workerId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStateException("Cannot complete booking in " + booking.getStatus() + " state. Must be in IN_PROGRESS state.");
        }

        if (workerId != null && booking.getWorkerId() != null && !booking.getWorkerId().equals(workerId)) {
            throw new InvalidBookingStateException("Worker ID does not match the assigned worker for this booking.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        // Cooperative 95/5 Split Financial Release
        log.info("Booking {} COMPLETED. Cooperative 95/5 Split triggered: Released 95% worker payout of ₹{} to worker {}. Retained 5% platform fee of ₹{}.",
                booking.getId(), booking.getWorkerPayout(), booking.getWorkerId(), booking.getPlatformFee());

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking cancelBooking(Long bookingId, CancelBookingDto dto) {
        Booking booking = getBookingById(bookingId);

        // Disallow cancellation if service is in progress or completed
        if (booking.getStatus() == BookingStatus.IN_PROGRESS || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new InvalidBookingStateException("Cannot cancel a booking that is already in progress or completed.");
        }

        // Must be in REQUESTED or ACCEPTED state
        if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidBookingStateException("Booking can only be cancelled when in REQUESTED or ACCEPTED state. Current state: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        if (dto != null && dto.getReason() != null) {
            booking.setCancellationReason(dto.getReason());
        }

        // Release escrow / pre-authorized payment back to customer
        log.info("Booking {} CANCELLED. Released escrow/pre-authorized payment of ₹{} back to customer {}.",
                booking.getId(), booking.getEstimatedPrice(), booking.getCustomerId());

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getCustomerBookings(Long customerId) {
        log.info("Fetching bookings for customer ID: {}", customerId);
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));
    }
}
