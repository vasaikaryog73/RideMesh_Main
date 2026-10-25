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
import com.project.ridemesh.clients.WalletServiceClient;
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
    private final WalletServiceClient walletServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DistanceCalculationService distanceCalculationService;
    private final java.util.Map<Long, java.util.Set<Long>> declinedRidesPerDriver = new java.util.concurrent.ConcurrentHashMap<>();

    public DriverRideServiceImpl(ModelMapper modelMapper, RideRepository rideRepository, RideRequestService rideRequestService, RideService rideService, UserServiceClient userServiceClient, WalletServiceClient walletServiceClient, DistanceCalculationService distanceCalculationService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.modelMapper = modelMapper;
        this.rideRepository = rideRepository;
        this.rideRequestService = rideRequestService;
        this.rideService = rideService;
        this.userServiceClient = userServiceClient;
        this.walletServiceClient = walletServiceClient;
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
        List<Ride> activeRides = rideRepository.findByDriverIdAndStatusIn(driverId, List.of(
                RideStatus.ACCEPTED, RideStatus.CONFIRMED, 
                RideStatus.IN_PROGRESS, RideStatus.ONGOING, 
                RideStatus.PENDING_CUSTOMER_APPROVAL
        ));
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

        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.CONFIRMED) {
            throw new RuntimeConflictException("Ride cannot be started from status: " + ride.getStatus());
        }

        if (!ride.getOtp().equals(otp)) {
            throw new RuntimeConflictException("Otp is not valid");
        }

        ride.setStartedAt(LocalDateTime.now());
        Ride startedRide = rideService.updateRideStatus(ride, RideStatus.IN_PROGRESS);

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
            RiderDto riderDto = userServiceClient.getRiderById(endedRide.getRiderId());
            DriverDto driverDto = userServiceClient.getDriverById(endedRide.getDriverId());
            Long riderUserId = (riderDto != null && riderDto.getUser() != null) ? riderDto.getUser().getId() : endedRide.getRiderId();
            Long driverUserId = (driverDto != null && driverDto.getUser() != null) ? driverDto.getUser().getId() : endedRide.getDriverId();

            Map<String, Object> payload = new HashMap<>();
            payload.put("rideId", endedRide.getId());
            payload.put("riderId", endedRide.getRiderId());
            payload.put("driverId", endedRide.getDriverId());
            payload.put("riderUserId", riderUserId);
            payload.put("driverUserId", driverUserId);
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

        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.CONFIRMED) {
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

        // Out of matchmaking when Unavailable
        if (!Boolean.TRUE.equals(driver.getAvailable())) {
            log.info("Driver ID " + driverId + " is UNAVAILABLE. Returning empty list of pending requests.");
            return java.util.Collections.emptyList();
        }

        List<RideRequest> pendingRequests = rideRequestService.findPendingRideRequests();
        List<RideRequestDto> availableRides = new ArrayList<>();
        String driverDistrict = driver.getDistrict();
        String driverVehicleType = driver.getVehicleType();

        java.util.Set<Long> declined = declinedRidesPerDriver.getOrDefault(driverId, java.util.Collections.emptySet());
        for (RideRequest req : pendingRequests) {
            // Filter out requests already declined by this driver
            if (declined.contains(req.getId())) {
                continue;
            }

            // Filter by district: only driver belonging to this request's pickup district can see it
            if (!com.project.ridemesh.utils.LocationUtil.isDistrictMatch(driverDistrict, req.getPickupDistrict())) {
                continue;
            }

            // Filter by vehicle type: request is for rickshaw, then car/van/bike/scooty driver should NOT get that request
            if (!com.project.ridemesh.utils.LocationUtil.isVehicleTypeMatch(driverVehicleType, req.getVehicleType())) {
                continue;
            }

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

        if (ride.getStatus() != RideStatus.IN_PROGRESS && ride.getStatus() != RideStatus.ONGOING) {
            throw new RuntimeConflictException("Ride cannot be completed from status: " + ride.getStatus());
        }

        ride.setDriverCompleted(true);
        ride.setEndedAt(java.time.LocalDateTime.now());

        if (Boolean.TRUE.equals(ride.getRiderCompleted())) {
            if (ride.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
                ride.setStatus(RideStatus.PAYMENT_COMPLETED);
            } else {
                ride.setStatus(RideStatus.COMPLETED);
            }
            Ride completedRide = rideRepository.save(ride);
            publishRideEnded(completedRide);
            return mapToRideDto(completedRide);
        } else {
            ride.setStatus(RideStatus.PENDING_CUSTOMER_APPROVAL);
            Ride savedRide = rideRepository.save(ride);
            return mapToRideDto(savedRide);
        }
    }

    @Override
    @Transactional
    public void declineRide(Long rideRequestId, Long driverId) {
        if (driverId != null && rideRequestId != null) {
            declinedRidesPerDriver.computeIfAbsent(driverId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(rideRequestId);
        }
        RideRequest req = rideRequestService.findRideRequestById(rideRequestId);
        if (req != null && (req.getStatus() == RideRequestStatus.PENDING || req.getStatus() == RideRequestStatus.REQUESTED)) {
            req.setStatus(RideRequestStatus.REJECTED);
            rideRequestService.updateRideRequest(req);
        }
        log.info("Driver " + driverId + " declined ride request ID: " + rideRequestId);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCompleteAbandonedPendingApprovals() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        List<Ride> pendingRides = rideRepository.findByStatus(RideStatus.PENDING_CUSTOMER_APPROVAL);
        for (Ride r : pendingRides) {
            if (r.getEndedAt() != null && r.getEndedAt().isBefore(threshold)) {
                r.setRiderCompleted(true);
                if (r.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
                    r.setStatus(RideStatus.PAYMENT_COMPLETED);
                } else {
                    r.setStatus(RideStatus.COMPLETED);
                }
                Ride completed = rideRepository.save(r);
                publishRideEnded(completed);
                log.info("Auto-completed ride ID " + r.getId() + " after 12 hours without customer confirmation.");
            }
        }
    }

    private void publishRideEnded(Ride endedRide) {
        try {
            Long riderUserId = endedRide.getRiderId();
            Long driverUserId = endedRide.getDriverId();
            try {
                RiderDto riderDto = userServiceClient.getRiderById(endedRide.getRiderId());
                if (riderDto != null && riderDto.getUser() != null) {
                    riderUserId = riderDto.getUser().getId();
                }
            } catch (Exception e) {
                log.warn("Could not fetch rider user ID: " + e.getMessage());
            }

            try {
                DriverDto driverDto = userServiceClient.getDriverById(endedRide.getDriverId());
                if (driverDto != null && driverDto.getUser() != null) {
                    driverUserId = driverDto.getUser().getId();
                }
            } catch (Exception e) {
                log.warn("Could not fetch driver user ID: " + e.getMessage());
            }

            // Settle wallet payment synchronously if payment method is WALLET
            if (endedRide.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
                try {
                    walletServiceClient.settleRidePayment(
                            endedRide.getId(),
                            riderUserId,
                            driverUserId,
                            endedRide.getFare(),
                            "WALLET"
                    );
                } catch (Exception e) {
                    log.error("Failed to settle wallet payment synchronously for ride " + endedRide.getId(), e);
                }
            }

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("rideId", endedRide.getId());
            payload.put("riderId", endedRide.getRiderId());
            payload.put("driverId", endedRide.getDriverId());
            payload.put("riderUserId", riderUserId);
            payload.put("driverUserId", driverUserId);
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
        List<Ride> rides = rideRepository.findByDriverIdAndStatusIn(driverId, List.of(
                RideStatus.ACCEPTED, RideStatus.CONFIRMED, 
                RideStatus.IN_PROGRESS, RideStatus.ONGOING, 
                RideStatus.PENDING_CUSTOMER_APPROVAL,
                RideStatus.COMPLETED
        ));
        if (rides.isEmpty()) return null;
        return mapToRideDto(rides.get(0));
    }

    @Override
    @Transactional
    public RideDto confirmCashReceived(Long rideId, Long driverId) {
        Ride ride = rideService.getRideById(rideId);
        if (!ride.getDriverId().equals(driverId)) {
            throw new RuntimeConflictException("Driver does not own this ride.");
        }
        ride.setCashReceived(true);
        ride.setStatus(RideStatus.PAYMENT_COMPLETED);
        Ride saved = rideRepository.save(ride);
        return mapToRideDto(saved);
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

        // Auto-sync rating from UserService if not yet recorded on Ride entity
        if (rideDto.getDriverRating() == null || rideDto.getDriverRating() == 0.0) {
            try {
                Double rating = userServiceClient.getDriverRatingForRide(ride.getId());
                if (rating != null && rating > 0.0) {
                    rideDto.setDriverRating(rating);
                    ride.setDriverRating(rating);
                    if (ride.getStatus() != RideStatus.RIDER_RATED) {
                        ride.setStatus(RideStatus.RIDER_RATED);
                    }
                    rideRepository.save(ride);
                }
            } catch (Exception ignored) {
            }
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
