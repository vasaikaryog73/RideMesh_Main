package com.project.ridemesh.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.clients.UserServiceClient;
import com.project.ridemesh.clients.WalletServiceClient;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.RideRequestDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.dto.WalletDto;
import com.project.ridemesh.entities.Ride;
import com.project.ridemesh.entities.RideRequest;
import com.project.ridemesh.entities.enums.RideRequestStatus;
import com.project.ridemesh.entities.enums.RideStatus;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.RideRepository;
import com.project.ridemesh.repositories.RideRequestRepository;
import com.project.ridemesh.services.DistanceCalculationService;
import com.project.ridemesh.services.RiderRideService;
import com.project.ridemesh.strategies.managers.DriverMatchingStrategyManager;
import com.project.ridemesh.strategies.managers.RideFareCalculationStrategyManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RiderRideServiceImpl implements RiderRideService {

    private static final Log log = LogFactory.getLog(RiderRideServiceImpl.class);
    private final ModelMapper modelMapper;
    private final RideRequestRepository rideRequestRepository;
    private final RideRepository rideRepository;
    private final RideFareCalculationStrategyManager rideFareCalculationStrategyManager;
    private final DriverMatchingStrategyManager driverMatchingStrategyManager;
    private final UserServiceClient userServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final DistanceCalculationService distanceCalculationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RiderRideServiceImpl(ModelMapper modelMapper, RideRequestRepository rideRequestRepository, RideRepository rideRepository, RideFareCalculationStrategyManager rideFareCalculationStrategyManager, DriverMatchingStrategyManager driverMatchingStrategyManager, UserServiceClient userServiceClient, WalletServiceClient walletServiceClient, DistanceCalculationService distanceCalculationService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.modelMapper = modelMapper;
        this.rideRequestRepository = rideRequestRepository;
        this.rideRepository = rideRepository;
        this.rideFareCalculationStrategyManager = rideFareCalculationStrategyManager;
        this.driverMatchingStrategyManager = driverMatchingStrategyManager;
        this.userServiceClient = userServiceClient;
        this.walletServiceClient = walletServiceClient;
        this.distanceCalculationService = distanceCalculationService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RideRequestDto requestRide(RideRequestDto rideRequestDto, Long riderId) {
        RideRequest rideRequest = modelMapper.map(rideRequestDto, RideRequest.class);
        rideRequest.setRiderId(riderId);
        rideRequest.setStatus(RideRequestStatus.PENDING);

        Double pickupLat = rideRequest.getPickUpLocation().getY();
        Double pickupLon = rideRequest.getPickUpLocation().getX();
        String[] locationDetails = resolveStateAndDistrict(pickupLat, pickupLon);
        rideRequest.setPickupState(locationDetails[0]);
        rideRequest.setPickupDistrict(locationDetails[1]);

        Double distance = distanceCalculationService.calculateDistance(rideRequest.getPickUpLocation(), rideRequest.getDropOffLocation());
        if (distance > 50.0) {
            throw new RuntimeConflictException("Ride distance exceeds the maximum limit of 50 km.");
        }
        String vehicleType = rideRequest.getVehicleType();
        if ("rickshaw".equalsIgnoreCase(vehicleType) && distance > 5.0) {
            throw new RuntimeConflictException("Rickshaws cannot service distances greater than 5 km.");
        }
        if (("scooter".equalsIgnoreCase(vehicleType) || "scooty".equalsIgnoreCase(vehicleType) || "bike".equalsIgnoreCase(vehicleType)) && distance > 15.0) {
            throw new RuntimeConflictException("Scooters/Bikes cannot service distances greater than 15 km.");
        }

        double fare = rideFareCalculationStrategyManager.rideFareCalculation().calculateFare(rideRequest);
        rideRequest.setFare(fare);

        if (rideRequest.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
            WalletDto wallet = walletServiceClient.getWalletBalance(riderId);
            if (wallet == null || wallet.getBalance() == null || wallet.getBalance() < fare) {
                throw new RuntimeConflictException("Insufficient wallet balance. Calculated fare is ₹" + String.format("%.2f", fare) + " but wallet balance is ₹" + (wallet != null ? String.format("%.2f", wallet.getBalance()) : "0.00"));
            }
        }

        RideRequest savedRequest = rideRequestRepository.save(rideRequest);

        // Fetch Rider rating to choose driver matching strategy
        RiderDto riderDto = userServiceClient.getRiderById(riderId);
        double riderRating = riderDto != null ? riderDto.getRating() : 5.0;

        List<DriverDto> matchingDrivers = driverMatchingStrategyManager
                .driverMatchingStrategy(riderRating)
                .findMatchingDrivers(savedRequest);

        // Publish notifications to the matched drivers
        List<String> driverEmails = matchingDrivers.stream()
                .filter(driver -> driver.getUser() != null && driver.getUser().getEmail() != null)
                .map(driver -> driver.getUser().getEmail())
                .collect(Collectors.toList());

        if (!driverEmails.isEmpty()) {
            sendRideRequestNotification(driverEmails, savedRequest.getId());
        }

        RideRequestDto responseDto = modelMapper.map(savedRequest, RideRequestDto.class);
        responseDto.setRider(riderDto);
        return responseDto;
    }

    @Override
    @Transactional
    public RideDto cancelRide(Long rideId, Long riderId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        if (!ride.getRiderId().equals(riderId)) {
            throw new RuntimeConflictException("Rider does not own this ride.");
        }

        if (ride.getStatus() != RideStatus.CONFIRMED && ride.getStatus() != RideStatus.ONGOING) {
            throw new RuntimeConflictException("Ride cannot be cancelled from status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        Ride savedRide = rideRepository.save(ride);

        // Publish event to Kafka to reset the driver availability to true
        kafkaTemplate.send("ride-cancelled-event", String.valueOf(ride.getDriverId()));

        return mapToRideDto(savedRide);
    }

    @Override
    public Page<RideDto> getAllMyRides(Long riderId, Pageable pageable) {
        return rideRepository.findByRiderId(riderId, pageable).map(this::mapToRideDto);
    }

    private void sendRideRequestNotification(List<String> driverEmails, Long rideRequestId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", java.util.UUID.randomUUID().toString());
            payload.put("rideRequestId", rideRequestId);
            payload.put("driverEmails", driverEmails);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("ride-request-notification", jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish ride-request-notification event", e);
        }
    }

    @Override
    @Transactional
    public RideDto completeRide(Long rideId, Long riderId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        if (!ride.getRiderId().equals(riderId)) {
            throw new RuntimeConflictException("Rider does not own this ride.");
        }

        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RuntimeConflictException("Ride cannot be completed from status: " + ride.getStatus());
        }

        if (ride.getPaymentMethod() != com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
            throw new RuntimeConflictException("Rider confirmation is only supported for WALLET payments.");
        }

        ride.setRiderCompleted(true);
        if (Boolean.TRUE.equals(ride.getDriverCompleted())) {
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
    public RideDto getActiveRide(Long riderId) {
        List<Ride> rides = rideRepository.findByRiderIdAndStatusIn(riderId, List.of(RideStatus.CONFIRMED, RideStatus.ONGOING));
        if (rides.isEmpty()) return null;
        return mapToRideDto(rides.get(0));
    }

    private RideDto mapToRideDto(Ride ride) {
        RideDto rideDto = modelMapper.map(ride, RideDto.class);
        
        // Fetch driver & rider profiles synchronously from UserService to populate response DTO
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

    private String[] resolveStateAndDistrict(double lat, double lon) {
        // 1. Bounding box check
        if ((lon >= 72.75 && lon <= 72.95) && (lat >= 19.00 && lat <= 19.30)) {
            return new String[]{"Maharashtra", "Mumbai Suburban"};
        }
        if ((lon >= 72.75 && lon <= 72.88) && (lat >= 18.88 && lat <= 19.03)) {
            return new String[]{"Maharashtra", "Mumbai"};
        }
        if ((lon >= 72.85 && lon <= 73.45) && (lat >= 19.00 && lat <= 19.65)) {
            return new String[]{"Maharashtra", "Thane"};
        }
        if ((lon >= 72.60 && lon <= 73.15) && (lat >= 19.40 && lat <= 20.35)) {
            return new String[]{"Maharashtra", "Palghar"};
        }
        if ((lon >= 73.30 && lon <= 75.10) && (lat >= 17.90 && lat <= 19.35)) {
            return new String[]{"Maharashtra", "Pune"};
        }
        if ((lon >= 73.20 && lon <= 74.90) && (lat >= 19.50 && lat <= 20.90)) {
            return new String[]{"Maharashtra", "Nashik"};
        }
        if ((lon >= 77.35 && lon <= 77.85) && (lat >= 12.70 && lat <= 13.25)) {
            return new String[]{"Karnataka", "Bengaluru Urban"};
        }
        if ((lon >= 77.15 && lon <= 77.95) && (lat >= 12.75 && lat <= 13.55)) {
            return new String[]{"Karnataka", "Bengaluru Rural"};
        }
        if ((lon >= 77.90 && lon <= 78.60) && (lat >= 12.75 && lat <= 13.45)) {
            return new String[]{"Karnataka", "Kolar"};
        }
        if ((lon >= 76.30 && lon <= 77.35) && (lat >= 13.00 && lat <= 14.35)) {
            return new String[]{"Karnataka", "Tumakuru"};
        }
        if ((lon >= 76.80 && lon <= 77.40) && (lat >= 28.40 && lat <= 28.90)) {
            return new String[]{"Delhi", "New Delhi"};
        }

        // 2. Fallback: Reverse Geocoding with Nominatim API
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "RideMesh-Cab-Booking-App");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f", lat, lon);
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                java.util.Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                java.util.Map address = (java.util.Map) response.getBody().get("address");
                if (address != null) {
                    String state = (String) address.get("state");
                    String district = (String) address.get("county");
                    if (district == null) district = (String) address.get("district");
                    if (district == null) district = (String) address.get("city_district");
                    if (district == null) district = (String) address.get("city");
                    if (district == null) district = (String) address.get("state_district");
                    
                    if (state != null && district != null) {
                        district = district.replace(" District", "").replace(" County", "").trim();
                        return new String[]{state, district};
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return new String[]{"Maharashtra", "Mumbai Suburban"}; // Default fallback
    }
}
