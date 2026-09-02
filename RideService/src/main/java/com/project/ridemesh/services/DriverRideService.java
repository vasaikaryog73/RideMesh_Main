package com.project.ridemesh.services;

import com.project.ridemesh.dto.RideDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project.ridemesh.dto.RideRequestDto;
import java.util.List;

public interface DriverRideService {
    RideDto acceptRide(Long rideRequestId, Long driverId);
    RideDto startRide(Long rideId, String otp, Long driverId);
    RideDto endRide(Long rideId, Long driverId);
    RideDto cancelRide(Long rideId, Long driverId);
    Page<RideDto> getAllMyRides(Long driverId, Pageable pageable);
    List<RideRequestDto> getPendingRidesNearDriver(Long driverId);
    RideDto completeRide(Long rideId, Long driverId);
    RideDto getActiveRide(Long driverId);
}
