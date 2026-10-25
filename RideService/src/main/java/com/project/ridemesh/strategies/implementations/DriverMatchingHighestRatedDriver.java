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
public class DriverMatchingHighestRatedDriver implements DriverMatchingStrategy {

    private final UserServiceClient userServiceClient;
    private final ModelMapper modelMapper;

    public DriverMatchingHighestRatedDriver(UserServiceClient userServiceClient, ModelMapper modelMapper) {
        this.userServiceClient = userServiceClient;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<DriverDto> findMatchingDrivers(RideRequest rideRequest) {
        String vehicleType = rideRequest.getVehicleType();
        List<String> districts = rideRequest.getPickupDistrict() != null 
                ? java.util.Collections.singletonList(rideRequest.getPickupDistrict()) 
                : java.util.Collections.emptyList();
        List<DriverDto> drivers = userServiceClient.getMatchingDrivers(districts, vehicleType);
        return drivers.stream()
                .filter(d -> com.project.ridemesh.utils.LocationUtil.isDistrictMatch(d.getDistrict(), rideRequest.getPickupDistrict()))
                .filter(d -> com.project.ridemesh.utils.LocationUtil.isVehicleTypeMatch(d.getVehicleType(), rideRequest.getVehicleType()))
                .collect(java.util.stream.Collectors.toList());
    }
}
