package com.project.ridemesh.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentStatus;
import com.project.ridemesh.entities.enums.TransactionMethod;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.PaymentRepository;
import com.project.ridemesh.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);
    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public RazorpayWebhookController(PaymentRepository paymentRepository, WalletService walletService, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received Razorpay Webhook notification");

        // Verify the signature if it is provided (in production it is mandatory)
        if (signature != null && !verifySignature(rawBody, signature, webhookSecret)) {
            log.error("Signature verification failed for Razorpay webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(rawBody, Map.class);
            Long rideId = extractRideId(payloadMap);
            String transactionId = extractTransactionId(payloadMap);

            if (rideId == null) {
                log.error("Could not extract rideId from Razorpay webhook payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("rideId not found in payload");
            }

            Payment payment = paymentRepository.findByRideId(rideId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride ID: " + rideId));

            if (payment.getPaymentStatus() == PaymentStatus.CONFIRMED) {
                log.info("Payment already confirmed for ride ID: " + rideId);
                return ResponseEntity.ok("Already processed");
            }

            // Transfer money to Driver's wallet (Fare - Platform Commission fee)
            double commission = payment.getAmount() * 0.3;
            double netAmount = payment.getAmount() - commission;

            walletService.addMoneyToWallet(
                    payment.getDriverId(),
                    netAmount,
                    transactionId != null ? transactionId : "RP_TXN_" + System.currentTimeMillis(),
                    payment.getRideId(),
                    TransactionMethod.RIDE
            );

            payment.setPaymentStatus(PaymentStatus.CONFIRMED);
            paymentRepository.save(payment);

            log.info("Razorpay payment successfully processed for ride ID: " + rideId);
            return ResponseEntity.ok("Payment confirmed");

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    private boolean verifySignature(String payload, String signature, String secret) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hash = sha256HMAC.doFinal(payload.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Long extractRideId(Map<String, Object> payloadMap) {
        // 1. Direct rideId parameter (for easy mock testing)
        if (payloadMap.containsKey("rideId")) {
            return ((Number) payloadMap.get("rideId")).longValue();
        }

        // 2. Standard Razorpay Webhook Event parsing
        if (payloadMap.containsKey("payload")) {
            Map<String, Object> payload = (Map<String, Object>) payloadMap.get("payload");
            if (payload.containsKey("payment")) {
                Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
                if (payment.containsKey("entity")) {
                    Map<String, Object> entity = (Map<String, Object>) payment.get("entity");
                    
                    // Look for rideId in notes metadata
                    if (entity.containsKey("notes")) {
                        Map<String, Object> notes = (Map<String, Object>) entity.get("notes");
                        if (notes.containsKey("rideId")) {
                            return Long.valueOf(String.valueOf(notes.get("rideId")));
                        }
                    }
                    
                    // Look for rideId in description
                    if (entity.containsKey("description")) {
                        String description = (String) entity.get("description");
                        // Find a pattern like "Ride: 45" or similar
                        try {
                            String cleaned = description.replaceAll("[^0-9]", "");
                            if (!cleaned.isEmpty()) {
                                return Long.valueOf(cleaned);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractTransactionId(Map<String, Object> payloadMap) {
        if (payloadMap.containsKey("payload")) {
            Map<String, Object> payload = (Map<String, Object>) payloadMap.get("payload");
            if (payload.containsKey("payment")) {
                Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
                if (payment.containsKey("entity")) {
                    Map<String, Object> entity = (Map<String, Object>) payment.get("entity");
                    if (entity.containsKey("id")) {
                        return (String) entity.get("id");
                    }
                }
            }
        }
        return null;
    }
}
