package com.project.ridemesh.services.implementations;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Rider;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.RiderRepository;
import com.project.ridemesh.services.RatingManagementService;
import com.project.ridemesh.services.RiderService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RiderServiceImpl implements RiderService {

    private final ModelMapper modelMapper;
    private final RiderRepository riderRepository;
    private final RatingManagementService ratingManagementService;

    public RiderServiceImpl(ModelMapper modelMapper, RiderRepository riderRepository, RatingManagementService ratingManagementService) {
        this.modelMapper = modelMapper;
        this.riderRepository = riderRepository;
        this.ratingManagementService = ratingManagementService;
    }

    @Override
    public RiderDto getRiderProfile() {
        Rider rider = getCurrentRider();
        return modelMapper.map(rider, RiderDto.class);
    }

    @Override
    public DriverDto rateDriver(Long rideId, Double rating) {
        Rider rider = getCurrentRider();
        return ratingManagementService.rateDriver(rideId, rider, rating);
    }

    @Override
    public void createNewRider(User savedUser) {
        Rider rider = new Rider
                .RiderBuilder()
                .setUser(savedUser)
                .setRating(0.0)
                .build();
        riderRepository.save(rider);
    }

    @Override
    public Rider getCurrentRider() {
        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return riderRepository
                .findByUser(user)
                .orElseThrow(()-> new AuthenticationServiceException("No Rider was found for user ID: " + user.getId()));
    }

    @Override
    public Rider getRiderById(Long id) {
        return riderRepository
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No Rider was found with ID: "+id));
    }

    @Override
    public Rider updateRating(Rider rider, Double rating) {
        rider.setRating(rating);
        return riderRepository.save(rider);
    }
}
