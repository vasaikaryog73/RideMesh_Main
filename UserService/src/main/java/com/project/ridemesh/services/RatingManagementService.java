package com.project.ridemesh.services;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.Rider;

public interface RatingManagementService {
    DriverDto rateDriver(Long rideId, Rider rider, Double rating);
    RiderDto rateRider(Long rideId, Driver driver, Double rating);
    void createNewRating(Long rideId, Long riderId, Long driverId);
}
