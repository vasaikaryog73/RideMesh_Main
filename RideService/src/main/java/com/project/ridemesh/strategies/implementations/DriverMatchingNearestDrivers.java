package com.project.ridemesh.strategies.implementations;

import com.project.ridemesh.clients.UserServiceClient;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.PointDto;
import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.strategies.DriverMatchingStrategy;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverMatchingNearestDrivers implements DriverMatchingStrategy {

    private final UserServiceClient userServiceClient;
    private final ModelMapper modelMapper;

    public DriverMatchingNearestDrivers(UserServiceClient userServiceClient, ModelMapper modelMapper) {
        this.userServiceClient = userServiceClient;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<DriverDto> findMatchingDrivers(RideRequest rideRequest) {
        String vehicleType = rideRequest.getVehicleType();
        return userServiceClient.getMatchingDrivers(java.util.Collections.emptyList(), vehicleType);
    }
}
