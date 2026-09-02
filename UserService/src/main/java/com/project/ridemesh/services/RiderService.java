package com.project.ridemesh.services;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Rider;
import com.project.ridemesh.entities.User;

public interface RiderService {
    RiderDto getRiderProfile();
    DriverDto rateDriver(Long rideId, Double rating);
    void createNewRider(User savedUser);
    Rider getCurrentRider();
    Rider getRiderById(Long id);
    Rider updateRating(Rider rider, Double rating);
}
