package com.project.ridemesh.strategies;

import com.project.ridemesh.entities.RideRequest;

public interface RideFareCalculation {
    double RIDE_FARE_MULTIPLIER = 10;
    double calculateFare(RideRequest rideRequest);
}
