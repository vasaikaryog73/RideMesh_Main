package com.project.ridemesh.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.services.DriverService;
import com.project.ridemesh.services.RatingManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserServiceConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserServiceConsumer.class);
    private final DriverService driverService;
    private final RatingManagementService ratingManagementService;
    private final ObjectMapper objectMapper;

    public UserServiceConsumer(DriverService driverService, RatingManagementService ratingManagementService, ObjectMapper objectMapper) {
        this.driverService = driverService;
        this.ratingManagementService = ratingManagementService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ride-accepted-event", groupId = "user-service-group")
    public void consumeRideAccepted(String message) {
        try {
            log.info("Received ride-accepted-event: " + message);
            Long driverId = Long.valueOf(message);
            Driver driver = driverService.getDriverById(driverId);
            driverService.updateDriverAvailability(driver, false);
        } catch (Exception e) {
            log.error("Error processing ride-accepted-event", e);
        }
    }

    @KafkaListener(topics = "ride-cancelled-event", groupId = "user-service-group")
    public void consumeRideCancelled(String message) {
        try {
            log.info("Received ride-cancelled-event: " + message);
            Long driverId = Long.valueOf(message);
            Driver driver = driverService.getDriverById(driverId);
            driverService.updateDriverAvailability(driver, true);
        } catch (Exception e) {
            log.error("Error processing ride-cancelled-event", e);
        }
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "ride-ended-event", groupId = "user-service-group")
    public void consumeRideEnded(String message) {
        try {
            log.info("Received ride-ended-event: " + message);
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            Long rideId = ((Number) payload.get("rideId")).longValue();
            Long riderId = ((Number) payload.get("riderId")).longValue();
            Long driverId = ((Number) payload.get("driverId")).longValue();

            // 1. Update driver availability to true
            Driver driver = driverService.getDriverById(driverId);
            driverService.updateDriverAvailability(driver, true);

            // 2. Create rating stub
            ratingManagementService.createNewRating(rideId, riderId, driverId);

        } catch (Exception e) {
            log.error("Error processing ride-ended-event", e);
        }
    }
}
