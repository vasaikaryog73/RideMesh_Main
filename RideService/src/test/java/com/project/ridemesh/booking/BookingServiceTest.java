package com.project.ridemesh.booking;

import com.project.ridemesh.booking.dto.AcceptBookingDto;
import com.project.ridemesh.booking.dto.CancelBookingDto;
import com.project.ridemesh.booking.dto.CreateBookingDto;
import com.project.ridemesh.booking.dto.DeclineBookingDto;
import com.project.ridemesh.booking.entities.Booking;
import com.project.ridemesh.booking.enums.BookingStatus;
import com.project.ridemesh.booking.exceptions.InvalidBookingStateException;
import com.project.ridemesh.booking.repositories.BookingRepository;
import com.project.ridemesh.booking.services.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private CreateBookingDto sampleCreateDto;

    @BeforeEach
    void setUp() {
        sampleCreateDto = new CreateBookingDto(
                101L,
                "Alice Johnson",
                "+919876543210",
                null,
                null,
                "Home Cleaning",
                "Deep cleaning for 2BHK apartment",
                "Powai, Mumbai",
                LocalDate.now().plusDays(1),
                "10:00 AM",
                2000.0
        );
    }

    @Test
    @DisplayName("Should create booking with 95/5 financial split and REQUESTED status")
    void testCreateBookingPricingSplit() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        Booking result = bookingService.createBooking(sampleCreateDto);

        assertNotNull(result);
        assertEquals(BookingStatus.REQUESTED, result.getStatus());
        assertEquals(2000.0, result.getEstimatedPrice());
        // 5% platform fee = 100.0, 95% worker payout = 1900.0
        assertEquals(100.0, result.getPlatformFee());
        assertEquals(1900.0, result.getWorkerPayout());
        assertEquals(101L, result.getCustomerId());
        assertEquals("Home Cleaning", result.getServiceCategory());
    }

    @Test
    @DisplayName("Worker accept booking sets workerId and ACCEPTED status")
    void testAcceptBookingSuccess() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.REQUESTED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        AcceptBookingDto dto = new AcceptBookingDto(55L, "Bob Handyman");
        Booking accepted = bookingService.acceptBooking(1L, dto);

        assertEquals(BookingStatus.ACCEPTED, accepted.getStatus());
        assertEquals(55L, accepted.getWorkerId());
        assertEquals("Bob Handyman", accepted.getWorkerName());
        assertNotNull(accepted.getAcceptedAt());
    }

    @Test
    @DisplayName("Worker decline booking sets REJECTED status")
    void testDeclineBookingSuccess() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.REQUESTED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        DeclineBookingDto dto = new DeclineBookingDto(55L, "Schedule conflict");
        Booking declined = bookingService.declineBooking(1L, dto);

        assertEquals(BookingStatus.REJECTED, declined.getStatus());
        assertEquals("Schedule conflict", declined.getCancellationReason());
        assertNotNull(declined.getRejectedAt());
    }

    @Test
    @DisplayName("Worker start service sets IN_PROGRESS status")
    void testStartBookingSuccess() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setWorkerId(55L);
        booking.setStatus(BookingStatus.ACCEPTED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking started = bookingService.startBooking(1L, 55L);

        assertEquals(BookingStatus.IN_PROGRESS, started.getStatus());
        assertNotNull(started.getStartedAt());
    }

    @Test
    @DisplayName("Worker complete service sets COMPLETED status and releases payout")
    void testCompleteBookingSuccess() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setWorkerId(55L);
        booking.setEstimatedPrice(2000.0);
        booking.setPlatformFee(100.0);
        booking.setWorkerPayout(1900.0);
        booking.setStatus(BookingStatus.IN_PROGRESS);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking completed = bookingService.completeBooking(1L, 55L);

        assertEquals(BookingStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    @DisplayName("Customer cancel from REQUESTED releases escrow and sets CANCELLED")
    void testCancelFromRequestedSuccess() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setCustomerId(101L);
        booking.setEstimatedPrice(2000.0);
        booking.setStatus(BookingStatus.REQUESTED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        CancelBookingDto dto = new CancelBookingDto(101L, "Plans changed");
        Booking cancelled = bookingService.cancelBooking(1L, dto);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Plans changed", cancelled.getCancellationReason());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    @DisplayName("Customer cancel throws 400 error if service is IN_PROGRESS")
    void testCancelFromInProgressThrowsException() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.IN_PROGRESS);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        InvalidBookingStateException ex = assertThrows(InvalidBookingStateException.class, () ->
                bookingService.cancelBooking(1L, new CancelBookingDto(101L, "Cancel now"))
        );

        assertTrue(ex.getMessage().contains("Cannot cancel a booking that is already in progress or completed"));
    }

    @Test
    @DisplayName("Customer cancel throws 400 error if service is COMPLETED")
    void testCancelFromCompletedThrowsException() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        InvalidBookingStateException ex = assertThrows(InvalidBookingStateException.class, () ->
                bookingService.cancelBooking(1L, new CancelBookingDto(101L, "Cancel now"))
        );

        assertTrue(ex.getMessage().contains("Cannot cancel a booking that is already in progress or completed"));
    }

    @Test
    @DisplayName("Worker pending bookings returns unassigned plus direct bookings")
    void testWorkerPendingBookings() {
        Booking unassigned = new Booking();
        unassigned.setId(1L);
        unassigned.setStatus(BookingStatus.REQUESTED);

        Booking direct = new Booking();
        direct.setId(2L);
        direct.setWorkerId(55L);
        direct.setStatus(BookingStatus.REQUESTED);

        when(bookingRepository.findPendingBookingsForWorker(BookingStatus.REQUESTED, 55L))
                .thenReturn(List.of(unassigned, direct));

        List<Booking> result = bookingService.getWorkerPendingBookings(55L);
        assertEquals(2, result.size());
    }
}
