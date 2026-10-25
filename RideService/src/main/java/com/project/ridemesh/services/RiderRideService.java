package com.project.ridemesh.services;

import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.RideRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RiderRideService {
    RideRequestDto requestRide(RideRequestDto rideRequestDto, Long riderId);
    RideDto cancelRide(Long rideId, Long riderId);
    Page<RideDto> getAllMyRides(Long riderId, Pageable pageable);
    RideDto completeRide(Long rideId, Long riderId);
    RideDto getActiveRide(Long riderId);
    void cancelRideRequest(Long rideRequestId, Long riderId);
    RideDto confirmCashPaid(Long rideId, Long riderId);
    RideDto finalizeUpiPayment(Long rideId);
    RideDto rateRide(Long rideId, Double rating);
}
