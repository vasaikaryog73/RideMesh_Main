package com.project.ridemesh.services.implementations;

import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.RideRequestRepository;
import com.project.ridemesh.services.RideRequestService;
import org.springframework.stereotype.Service;

import com.project.ridemesh.entities.enums.RideRequestStatus;
import java.util.List;

@Service
public class RideRequestServiceImpl implements RideRequestService {

    private final RideRequestRepository rideRequestRepository;

    public RideRequestServiceImpl(RideRequestRepository rideRequestRepository) {
        this.rideRequestRepository = rideRequestRepository;
    }

    @Override
    public RideRequest findRideRequestById(Long id) {
        return rideRequestRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("No ride request was found with ID: "+id));
    }

    @Override
    public void updateRideRequest(RideRequest rideRequest) {
        rideRequestRepository
                .findById(rideRequest.getId())
                .orElseThrow(()-> new ResourceNotFoundException("No ride request was found with ID: "+rideRequest.getId()));
        rideRequestRepository.save(rideRequest);
    }

    @Override
    public List<RideRequest> findPendingRideRequests() {
        return rideRequestRepository.findByStatus(RideRequestStatus.PENDING);
    }
}
