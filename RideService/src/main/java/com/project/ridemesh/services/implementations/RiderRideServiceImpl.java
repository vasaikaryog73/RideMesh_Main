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
    private final com.project.ridemesh.services.GeminiLocationClassifierService geminiLocationClassifierService;

    public RiderRideServiceImpl(ModelMapper modelMapper, RideRequestRepository rideRequestRepository, RideRepository rideRepository, RideFareCalculationStrategyManager rideFareCalculationStrategyManager, DriverMatchingStrategyManager driverMatchingStrategyManager, UserServiceClient userServiceClient, WalletServiceClient walletServiceClient, DistanceCalculationService distanceCalculationService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, com.project.ridemesh.services.GeminiLocationClassifierService geminiLocationClassifierService) {
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
        this.geminiLocationClassifierService = geminiLocationClassifierService;
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

        if (rideRequest.getDropOffLocation() != null) {
            Double dropoffLat = rideRequest.getDropOffLocation().getY();
            Double dropoffLon = rideRequest.getDropOffLocation().getX();
            String[] dropoffDetails = resolveStateAndDistrict(dropoffLat, dropoffLon);
            rideRequest.setDropoffState(dropoffDetails[0]);
            rideRequest.setDropoffDistrict(dropoffDetails[1]);
        }

        if (rideRequestDto.getPickupAddress() != null && !rideRequestDto.getPickupAddress().trim().isEmpty()) {
            rideRequest.setPickupAddress(rideRequestDto.getPickupAddress().trim());
        }
        if (rideRequestDto.getDropoffAddress() != null && !rideRequestDto.getDropoffAddress().trim().isEmpty()) {
            rideRequest.setDropoffAddress(rideRequestDto.getDropoffAddress().trim());
        }

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

        // Fetch Rider profile for rating and user mapping
        RiderDto riderDto = userServiceClient.getRiderById(riderId);
        Long userId = (riderDto != null && riderDto.getUser() != null) ? riderDto.getUser().getId() : riderId;

        if (rideRequest.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
            WalletDto wallet = walletServiceClient.getWalletBalance(userId);
            if (wallet == null || wallet.getBalance() == null || wallet.getBalance() < fare) {
                throw new RuntimeConflictException("Insufficient wallet balance. Calculated fare is ₹" + String.format("%.2f", fare) + " but wallet balance is ₹" + (wallet != null ? String.format("%.2f", wallet.getBalance()) : "0.00"));
            }
        }

        RideRequest savedRequest = rideRequestRepository.save(rideRequest);

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

        // If status is IN_PROGRESS or COMPLETED, throw a 400 Bad Request error
        if (ride.getStatus() == RideStatus.IN_PROGRESS || ride.getStatus() == RideStatus.ONGOING ||
            ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.PAYMENT_COMPLETED ||
            ride.getStatus() == RideStatus.ENDED) {
            throw new com.project.ridemesh.booking.exceptions.InvalidBookingStateException("Cannot cancel a ride that is already in progress or completed.");
        }

        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.CONFIRMED) {
            throw new com.project.ridemesh.booking.exceptions.InvalidBookingStateException("Ride can only be cancelled when in REQUESTED or ACCEPTED state. Current state: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        Ride savedRide = rideRepository.save(ride);

        // Publish event to Kafka to reset the driver availability to true
        kafkaTemplate.send("ride-cancelled-event", String.valueOf(ride.getDriverId()));

        return mapToRideDto(savedRide);
    }

    @Override
    @Transactional
    public void cancelRideRequest(Long rideRequestId, Long riderId) {
        RideRequest request = rideRequestRepository.findById(rideRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride request not found with ID: " + rideRequestId));

        if (!request.getRiderId().equals(riderId)) {
            throw new RuntimeConflictException("Rider does not own this ride request.");
        }

        if (request.getStatus() != RideRequestStatus.PENDING && request.getStatus() != RideRequestStatus.REQUESTED) {
            throw new RuntimeConflictException("Ride request cannot be cancelled from status: " + request.getStatus());
        }

        request.setStatus(RideRequestStatus.CANCELLED);
        rideRequestRepository.save(request);
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

        if (ride.getStatus() != RideStatus.IN_PROGRESS && ride.getStatus() != RideStatus.ONGOING 
                && ride.getStatus() != RideStatus.PENDING_CUSTOMER_APPROVAL) {
            throw new com.project.ridemesh.booking.exceptions.InvalidBookingStateException("Ride cannot be confirmed from status: " + ride.getStatus());
        }

        // If payment method is WALLET, ensure rider wallet has sufficient balance
        if (ride.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
            Long riderUserId = riderId;
            try {
                RiderDto riderDto = userServiceClient.getRiderById(riderId);
                if (riderDto != null && riderDto.getUser() != null) {
                    riderUserId = riderDto.getUser().getId();
                }
            } catch (Exception e) {
                log.warn("Could not fetch rider user ID: " + e.getMessage());
            }

            com.project.ridemesh.dto.WalletDto wallet = walletServiceClient.getWalletBalance(riderUserId);
            if (wallet == null || wallet.getBalance() == null || wallet.getBalance() < ride.getFare()) {
                double currentBal = (wallet != null && wallet.getBalance() != null) ? wallet.getBalance() : 0.0;
                throw new RuntimeConflictException("Insufficient wallet balance (₹" + String.format("%.2f", currentBal)
                        + "). Trip fare is ₹" + String.format("%.2f", ride.getFare()) + ". Please load funds into your wallet to confirm.");
            }
        }

        ride.setRiderCompleted(true);
        ride.setEndedAt(java.time.LocalDateTime.now());

        if (Boolean.TRUE.equals(ride.getDriverCompleted())) {
            if (ride.getPaymentMethod() == com.project.ridemesh.entities.enums.PaymentMethod.WALLET) {
                ride.setStatus(RideStatus.PAYMENT_COMPLETED);
            } else {
                ride.setStatus(RideStatus.COMPLETED);
            }
            Ride completedRide = rideRepository.save(ride);
            publishRideEnded(completedRide);
            return mapToRideDto(completedRide);
        } else {
            // Driver hasn't confirmed yet
            Ride saved = rideRepository.save(ride);
            return mapToRideDto(saved);
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
    public RideDto getActiveRide(Long riderId) {
        List<Ride> rides = rideRepository.findByRiderIdAndStatusIn(riderId, List.of(
                RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.CONFIRMED,
                RideStatus.IN_PROGRESS, RideStatus.ONGOING, 
                RideStatus.PENDING_CUSTOMER_APPROVAL,
                RideStatus.COMPLETED
        ));
        if (rides.isEmpty()) return null;
        return mapToRideDto(rides.get(0));
    }

    @Override
    @Transactional
    public RideDto confirmCashPaid(Long rideId, Long riderId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));
        if (!ride.getRiderId().equals(riderId)) {
            throw new RuntimeConflictException("Rider does not own this ride.");
        }
        ride.setCashPaid(true);
        Ride saved = rideRepository.save(ride);
        return mapToRideDto(saved);
    }

    @Override
    @Transactional
    public RideDto finalizeUpiPayment(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));
        ride.setStatus(RideStatus.PAYMENT_COMPLETED);
        Ride saved = rideRepository.save(ride);
        return mapToRideDto(saved);
    }

    @Override
    @Transactional
    public RideDto rateRide(Long rideId, Double rating) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));
        ride.setDriverRating(rating);
        ride.setStatus(RideStatus.RIDER_RATED);
        Ride saved = rideRepository.save(ride);
        return mapToRideDto(saved);
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

    private String[] resolveStateAndDistrict(double lat, double lon) {
        // 1. Google Gemini AI location classification for State & District
        try {
            String[] geminiResult = geminiLocationClassifierService.classifyLocation(lat, lon);
            if (geminiResult != null && geminiResult.length == 2 && geminiResult[0] != null && geminiResult[1] != null) {
                log.info("Classified pickup coordinates via Gemini AI: State=" + geminiResult[0] + ", District=" + geminiResult[1]);
                return geminiResult;
            }
        } catch (Exception e) {
            log.warn("Gemini AI classification failed: " + e.getMessage() + ". Falling back to local rules.");
        }

        // 2. Bounding box check
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
