package com.project.ridemesh.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.repositories.PaymentRepository;
import com.project.ridemesh.services.PaymentService;
import com.project.ridemesh.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WalletServiceConsumer {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceConsumer.class);
    private final WalletService walletService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public WalletServiceConsumer(WalletService walletService, PaymentService paymentService, PaymentRepository paymentRepository, ObjectMapper objectMapper) {
        this.walletService = walletService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user-signup-event", groupId = "wallet-service-group")
    public void consumeUserSignup(String message) {
        try {
            log.info("Received user-signup-event for userId: " + message);
            Long userId = Long.valueOf(message.trim());
            walletService.createNewWallet(userId);
        } catch (Exception e) {
            log.error("Error processing user-signup-event", e);
        }
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "ride-ended-event", groupId = "wallet-service-group")
    public void consumeRideEnded(String message) {
        try {
            log.info("Received ride-ended-event: " + message);
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);

            Long rideId = ((Number) payload.get("rideId")).longValue();
            Long riderId = ((Number) payload.get("riderId")).longValue();
            Long driverId = ((Number) payload.get("driverId")).longValue();
            Double fare = ((Number) payload.get("fare")).doubleValue();
            String paymentMethod = (String) payload.get("paymentMethod");

            // Idempotency: Check if payment already exists for this ride
            if (paymentRepository.findByRideId(rideId).isPresent()) {
                log.info("Duplicate ride-ended-event detected for rideId: " + rideId + ". Discarding event.");
                return;
            }

            try {
                Payment payment = paymentService.createNewPayment(rideId, driverId, riderId, fare, paymentMethod);
                paymentService.processPayment(payment);
            } catch (DataIntegrityViolationException ex) {
                // Concurrent event processing caught by unique constraint on rideId
                log.info("Ignoring duplicate concurrent payment creation for rideId: " + rideId);
            }

        } catch (Exception e) {
            log.error("Error processing ride-ended-event", e);
        }
    }
}
