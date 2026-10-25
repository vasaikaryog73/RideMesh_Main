package com.project.ridemesh.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.clients.UserServiceClient;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Ride;
import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.entities.enums.RideRequestStatus;
import com.project.ridemesh.entities.enums.RideStatus;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.RideRepository;
import com.project.ridemesh.services.RideRequestService;
import com.project.ridemesh.services.RideService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.project.ridemesh.utils.GenerateOtp.generateOtp;

@Service
public class RideServiceImpl implements RideService {

    private static final Log log = LogFactory.getLog(RideServiceImpl.class);
    private final RideRepository rideRepository;
    private final RideRequestService rideRequestService;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RideServiceImpl(RideRepository rideRepository, RideRequestService rideRequestService, UserServiceClient userServiceClient, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.rideRepository = rideRepository;
        this.rideRequestService = rideRequestService;
        this.userServiceClient = userServiceClient;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Ride getRideById(Long rideId) {
        return rideRepository
                .findById(rideId)
                .orElseThrow(()-> new ResourceNotFoundException("No ride was found with ID: "+rideId));
    }

    @Override
    public Ride createNewRide(RideRequest rideRequest, DriverDto driverDto) {
        rideRequest.setStatus(RideRequestStatus.CONFIRMED);
        
        Ride ride = new Ride();
        ride.setPickUpLocation(rideRequest.getPickUpLocation());
        ride.setDropOffLocation(rideRequest.getDropOffLocation());
        ride.setRiderId(rideRequest.getRiderId());
        ride.setDriverId(driverDto.getId());
        ride.setPaymentMethod(rideRequest.getPaymentMethod());
        ride.setFare(rideRequest.getFare());
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setStartedAt(null);
        ride.setVehicleType(rideRequest.getVehicleType());
        ride.setOtp(generateOtp());
        ride.setPickupState(rideRequest.getPickupState());
        ride.setPickupDistrict(rideRequest.getPickupDistrict());
        ride.setDropoffState(rideRequest.getDropoffState());
        ride.setDropoffDistrict(rideRequest.getDropoffDistrict());
        ride.setPickupAddress(rideRequest.getPickupAddress());
        ride.setDropoffAddress(rideRequest.getDropoffAddress());
        
        rideRequestService.updateRideRequest(rideRequest);
        Ride savedRide = rideRepository.save(ride);

        // Fetch rider email from UserService to publish OTP email alert
        RiderDto riderDto = userServiceClient.getRiderById(rideRequest.getRiderId());
        if (riderDto != null && riderDto.getUser() != null) {
            sendOtpEmailToRider(riderDto.getUser().getEmail(), savedRide.getOtp());
        } else {
            log.warn("Could not retrieve rider email for ride request ID: " + rideRequest.getId() + ", email alert skipped.");
        }

        return savedRide;
    }

    @Override
    public Ride updateRideStatus(Ride ride, RideStatus rideStatus) {
        ride.setStatus(rideStatus);
        return rideRepository.save(ride);
    }

    @Override
    public Page<Ride> getAllRidesOfRider(Long riderId, Pageable pageRequest) {
        return rideRepository.findByRiderId(riderId, pageRequest);
    }

    @Override
    public Page<Ride> getAllRidesOfDriver(Long driverId, Pageable pageRequest) {
        return rideRepository.findByDriverId(driverId, pageRequest);
    }

    private void sendOtpEmailToRider(String riderEmail, String otp) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("eventId", java.util.UUID.randomUUID().toString());
            payload.put("email", riderEmail);
            payload.put("otp", otp);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("ride-otp-notification", jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish ride-otp-notification event", e);
        }
    }
}
