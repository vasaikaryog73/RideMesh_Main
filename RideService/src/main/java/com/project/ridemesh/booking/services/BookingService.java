package com.project.ridemesh.booking.services;

import com.project.ridemesh.booking.dto.AcceptBookingDto;
import com.project.ridemesh.booking.dto.CancelBookingDto;
import com.project.ridemesh.booking.dto.CreateBookingDto;
import com.project.ridemesh.booking.dto.DeclineBookingDto;
import com.project.ridemesh.booking.entities.Booking;

import java.util.List;

public interface BookingService {

    Booking createBooking(CreateBookingDto dto);

    List<Booking> getWorkerPendingBookings(Long workerId);

    Booking acceptBooking(Long bookingId, AcceptBookingDto dto);

    Booking declineBooking(Long bookingId, DeclineBookingDto dto);

    Booking startBooking(Long bookingId, Long workerId);

    Booking completeBooking(Long bookingId, Long workerId);

    Booking cancelBooking(Long bookingId, CancelBookingDto dto);

    List<Booking> getCustomerBookings(Long customerId);

    Booking getBookingById(Long bookingId);
}
