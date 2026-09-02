package com.project.ridemesh.services.implementations;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.DriverRepository;
import com.project.ridemesh.services.DriverService;
import com.project.ridemesh.services.RatingManagementService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final RatingManagementService ratingManagementService;
    private final ModelMapper modelMapper;

    public DriverServiceImpl(DriverRepository driverRepository, RatingManagementService ratingManagementService, ModelMapper modelMapper) {
        this.driverRepository = driverRepository;
        this.ratingManagementService = ratingManagementService;
        this.modelMapper = modelMapper;
    }

    @Override
    public DriverDto getDriverProfile() {
        Driver driver = getCurrentDriver();
        return modelMapper.map(driver, DriverDto.class);
    }

    @Override
    public RiderDto rateRider(Long rideId, Double rating) {
        Driver driver = getCurrentDriver();
        return ratingManagementService.rateRider(rideId, driver, rating);
    }

    @Override
    public Driver getCurrentDriver() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return driverRepository
                .findByUser(user)
                .orElseThrow(()-> new AuthenticationServiceException("No driver was found for user ID: " + user.getId()));
    }

    @Override
    public Driver getDriverById(Long driverId) {
        return driverRepository
                .findById(driverId)
                .orElseThrow(()-> new ResourceNotFoundException("No driver was found with ID: "+driverId));
    }

    @Override
    public Driver updateRating(Driver driver, Double rating) {
        driver.setRating(rating);
        return driverRepository.save(driver);
    }

    @Override
    public Driver updateDriverAvailability(Driver driver, boolean available) {
        driver.setAvailable(available);
        return driverRepository.save(driver);
    }

    @Override
    public Driver createNewDriver(Driver createDriver) {
        return driverRepository.save(createDriver);
    }
}
