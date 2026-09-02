package com.project.ridemesh.services;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Driver;

public interface DriverService {
    DriverDto getDriverProfile();
    RiderDto rateRider(Long rideId, Double rating);
    Driver getCurrentDriver();
    Driver getDriverById(Long driverId);
    Driver updateRating(Driver driver, Double rating);
    Driver updateDriverAvailability(Driver driver, boolean available);
    Driver createNewDriver(Driver createDriver);
}
