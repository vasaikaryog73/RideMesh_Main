package com.project.ridemesh.strategies;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.entities.RideRequest;
import java.util.List;

public interface DriverMatchingStrategy {
    List<DriverDto> findMatchingDrivers(RideRequest rideRequest);
}
