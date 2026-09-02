package com.project.ridemesh.strategies.managers;

import com.project.ridemesh.strategies.DriverMatchingStrategy;
import com.project.ridemesh.strategies.implementations.DriverMatchingHighestRatedDriver;
import com.project.ridemesh.strategies.implementations.DriverMatchingNearestDrivers;
import org.springframework.stereotype.Component;

@Component
public class DriverMatchingStrategyManager {

    private final DriverMatchingHighestRatedDriver driverMatchingHighestRatedDriver;
    private final DriverMatchingNearestDrivers driverMatchingNearestDrivers;

    public DriverMatchingStrategyManager(DriverMatchingHighestRatedDriver driverMatchingHighestRatedDriver, DriverMatchingNearestDrivers driverMatchingNearestDrivers) {
        this.driverMatchingHighestRatedDriver = driverMatchingHighestRatedDriver;
        this.driverMatchingNearestDrivers = driverMatchingNearestDrivers;
    }

    public DriverMatchingStrategy driverMatchingStrategy(double riderRating){
        if (riderRating > 4.8){
            return driverMatchingHighestRatedDriver;
        } else {
            return driverMatchingNearestDrivers;
        }
    }
}
