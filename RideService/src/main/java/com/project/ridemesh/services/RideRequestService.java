package com.project.ridemesh.services;

import com.project.ridemesh.entities.RideRequest;

import java.util.List;

public interface RideRequestService {
    RideRequest findRideRequestById(Long id);
    void updateRideRequest(RideRequest rideRequest);
    List<RideRequest> findPendingRideRequests();
}
