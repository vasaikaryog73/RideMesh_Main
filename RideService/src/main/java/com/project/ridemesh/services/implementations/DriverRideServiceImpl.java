package com.project.ridemesh.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.clients.UserServiceClient;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.dto.RideRequestDto;
import com.project.ridemesh.entities.Ride;
import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.entities.enums.RideRequestStatus;
import com.project.ridemesh.entities.enums.RideStatus;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.RideRepository;
import com.project.ridemesh.services.DistanceCalculationService;
import com.project.ridemesh.services.DriverRideService;
import com.project.ridemesh.services.RideRequestService;
import com.project.ridemesh.services.RideService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DriverRideServiceImpl implements DriverRideService {

    private static final Log log = LogFactory.getLog(DriverRideServiceImpl.class);
    private final ModelMapper modelMapper;
    private final RideRepository rideRepository;
    private final RideRequestService rideRequestService;
    private final RideService rideService;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DistanceCalculationService distanceCalculationService;

    public DriverRideServiceImpl(ModelMapper modelMapper, RideRepository rideRepository, RideRequestService rideRequestService, RideService rideService, UserServiceClient userServiceClient, DistanceCalculationService distanceCalculationService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.modelMapper = modelMapper;
        this.rideRepository = rideRepository;
        this.rideRequestService = rideRequestService;
        this.rideService = rideService;
        this.userServiceClient = userServiceClient;
        this.distanceCalculationService = distanceCalculationService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RideDto acceptRide(Long rideRequestId, Long driverId) {
        RideRequest rideRequest = rideRequestService.findRideRequestById(rideRequestId);
        if (rideRequest.getStatus() != RideRequestStatus.PENDING) {
            throw new RuntimeConflictException("RideRequest cannot be accepted, status: " + rideRequest.getStatus());
        }

        // Fetch driver profile from UserService to check availability
        DriverDto driverDto = userServiceClient.getDriverById(driverId);
        if (driverDto == null) {
            throw new ResourceNotFoundException("Driver profile not found for ID: " + driverId);
        }

        // Check if driver currently has an active ride in RideService
        List<Ride> activeRides = rideRepository.findByDriverIdAndStatusIn(driverId, List.of(RideStatus.CONFIRMED, RideStatus.ONGOING));
        if (!activeRides.isEmpty()) {
            throw new RuntimeConflictException("Driver already has an active ride in progress.");
        }

        Ride ride = rideService.createNewRide(rideRequest, driverDto);

        // Publish event to lock driver availability in UserService
        try {
            kafkaTemplate.send("ride-accepted-event", String.valueOf(driverId));
        } catch (Exception e) {
            log.warn("Failed to publish ride-accepted-event: " + e.getMessage());
        }

        return mapToRideDto(ride);
    }

    @Override
    @Transactional
    public RideDto startRide(Long rideId, String otp, Long driverId) {
        Ride ride = rideService.getRideById(rideId);

        if (!ride.getDriverId().equals(driverId)) {
            throw new RuntimeConflictException("Driver does not own this ride.");
        }

        if (ride.getStatus() != RideStatus.CONFIRMED) {
            throw new RuntimeConflictException("Ride cannot be started from status: " + ride.getStatus());
        }

        if (!ride.getOtp().equals(otp)) {
            throw new RuntimeConflictException("Otp is not valid");
        }

        ride.setStartedAt(LocalDateTime.now());
        Ride startedRide = rideService.updateRideStatus(ride, RideStatus.ONGOING);

        return mapToRideDto(startedRide);
    }

    @Override
    @Transactional
    public RideDto endRide(Long rideId, Long driverId) {
        Ride ride = rideService.getRideById(rideId);

        if (!ride.getDriverId().equals(driverId)) {
            throw new RuntimeConflictException("Driver does not own this ride.");
        }

        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RuntimeConflictException("Ride cannot be ended from status: " + ride.getStatus());
        }

        ride.setEndedAt(LocalDateTime.now());
        Ride endedRide = rideService.updateRideStatus(ride, RideStatus.ENDED);

        // Publish ride-ended-event to notify UserService & WalletService
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("rideId", endedRide.getId());
            payload.put("riderId", endedRide.getRiderId());
            payload.put("driverId", endedRide.getDriverId());
            payload.put("fare", endedRide.getFare());
            payload.put("paymentMethod", endedRide.getPaymentMethod().name());

            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("ride-ended-event", jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish ride-ended-event", e);
        }

        return mapToRideDto(endedRide);
    }

    @Override
    @Transactional
    public RideDto cancelRide(Long rideId, Long driverId) {
        Ride ride = rideService.getRideById(rideId);

        if (!ride.getDriverId().equals(driverId)) {
            throw new RuntimeConflictException("Driver does not own this ride.");
        }

        if (ride.getStatus() != RideStatus.CONFIRMED) {
            throw new RuntimeConflictException("Ride cannot be cancelled from status: " + ride.getStatus());
        }

        Ride cancelledRide = rideService.updateRideStatus(ride, RideStatus.CANCELLED);

        // Publish ride-cancelled-event to unlock driver availability in UserService
        kafkaTemplate.send("ride-cancelled-event", String.valueOf(driverId));

        return mapToRideDto(cancelledRide);
    }

    @Override
    public Page<RideDto> getAllMyRides(Long driverId, Pageable pageable) {
        return rideRepository.findByDriverId(driverId, pageable).map(this::mapToRideDto);
    }

    @Override
    public List<RideRequestDto> getPendingRidesNearDriver(Long driverId) {
        DriverDto driver = userServiceClient.getDriverById(driverId);
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for ID: " + driverId);
        }

        List<RideRequest> pendingRequests = rideRequestService.findPendingRideRequests();
        List<RideRequestDto> availableRides = new ArrayList<>();

        for (RideRequest req : pendingRequests) {
            RideRequestDto dto = modelMapper.map(req, RideRequestDto.class);
            try {
                RiderDto rider = userServiceClient.getRiderById(req.getRiderId());
                dto.setRider(rider);
            } catch (Exception e) {
                // Ignore if rider fetch fails
            }
            availableRides.add(dto);
        }
        return availableRides;
    }

    @Override
    @Transactional
    public RideDto completeRide(Long rideId, Long driverId) {
        Ride ride = rideService.getRideById(rideId);

        if (!ride.getDriverId().equals(driverId)) {
            throw new RuntimeConflictException("Driver does not own this ride.");
        }

        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RuntimeConflictException("Ride cannot be completed from status: " + ride.getStatus());
        }

        if (ride.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.CASH) {
            ride.setEndedAt(java.time.LocalDateTime.now());
            Ride completedRide = rideService.updateRideStatus(ride, RideStatus.PAYMENT_COMPLETED);
            publishRideEnded(completedRide);
            return mapToRideDto(completedRide);
        } else {
            ride.setDriverCompleted(true);
            if (Boolean.TRUE.equals(ride.getRiderCompleted())) {
                ride.setEndedAt(java.time.LocalDateTime.now());
                ride.setStatus(RideStatus.PAYMENT_COMPLETED);
                Ride completedRide = rideRepository.save(ride);
                publishRideEnded(completedRide);
                return mapToRideDto(completedRide);
            } else {
                Ride savedRide = rideRepository.save(ride);
                return mapToRideDto(savedRide);
            }
        }
    }

    private void publishRideEnded(Ride endedRide) {
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("rideId", endedRide.getId());
            payload.put("riderId", endedRide.getRiderId());
            payload.put("driverId", endedRide.getDriverId());
            payload.put("fare", endedRide.getFare());
            payload.put("paymentMethod", endedRide.getPaymentMethod().name());

            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("ride-ended-event", jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish ride-ended-event", e);
        }
    }

    @Override
    public RideDto getActiveRide(Long driverId) {
        List<Ride> rides = rideRepository.findByDriverIdAndStatusIn(driverId, List.of(RideStatus.CONFIRMED, RideStatus.ONGOING));
        if (rides.isEmpty()) return null;
        return mapToRideDto(rides.get(0));
    }

    private RideDto mapToRideDto(Ride ride) {
        RideDto rideDto = modelMapper.map(ride, RideDto.class);
        
        try {
            RiderDto riderDto = userServiceClient.getRiderById(ride.getRiderId());
            DriverDto driverDto = userServiceClient.getDriverById(ride.getDriverId());
            rideDto.setRider(riderDto);
            rideDto.setDriver(driverDto);
        } catch (Exception e) {
            log.warn("Failed to populate rider/driver profiles in RideDto: " + e.getMessage());
        }
        return rideDto;
    }

    private boolean isDistrictEligible(String driverDistrict, String rideDistrict, String vehicleType) {
        if (driverDistrict == null || rideDistrict == null) return false;
        
        if (driverDistrict.equalsIgnoreCase(rideDistrict)) {
            return true;
        }
        
        if (vehicleType != null && (vehicleType.toLowerCase().contains("car") || vehicleType.toLowerCase().contains("van"))) {
            List<String> adj = com.project.ridemesh.utils.LocationUtil.getAdjacentDistricts(driverDistrict);
            for (String a : adj) {
                if (a.equalsIgnoreCase(rideDistrict)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
