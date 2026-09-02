package com.project.ridemesh.strategies.implementations;

import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.services.DistanceCalculationService;
import com.project.ridemesh.strategies.RideFareCalculation;
import org.springframework.stereotype.Service;

@Service
public class RideFareSurgePricing implements RideFareCalculation {

    private final DistanceCalculationService distanceCalculationService;
    private static final double SURGE_FACTOR = 2.0;

    public RideFareSurgePricing(DistanceCalculationService distanceCalculationService) {
        this.distanceCalculationService = distanceCalculationService;
    }

    @Override
    public double calculateFare(RideRequest rideRequest) {
        Double distance = distanceCalculationService.calculateDistance(rideRequest.getPickUpLocation(), rideRequest.getDropOffLocation());
        double rate = getVehicleRate(rideRequest.getVehicleType());
        return distance * rate * SURGE_FACTOR;
    }

    private double getVehicleRate(String vehicleType) {
        if (vehicleType == null) return 25.0;
        switch (vehicleType.toLowerCase()) {
            case "rickshaw": return 20.0;
            case "scooter":
            case "scooty": return 10.0;
            case "bike": return 15.0;
            case "car": return 25.0;
            case "6 seater van": return 40.0;
            case "8 seater van": return 50.0;
            default: return 25.0;
        }
    }
}
