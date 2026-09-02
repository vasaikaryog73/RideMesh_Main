package com.project.ridemesh.services;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.entities.Ride;
import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.entities.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RideService {
    Ride getRideById(Long rideId);
    Ride createNewRide(RideRequest rideRequest, DriverDto driverDto);
    Ride updateRideStatus(Ride ride, RideStatus rideStatus);
    Page<Ride> getAllRidesOfRider(Long riderId, Pageable pageRequest);
    Page<Ride> getAllRidesOfDriver(Long driverId, Pageable pageRequest);
}
