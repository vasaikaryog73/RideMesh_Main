package com.project.ridemesh.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.entities.NotificationLog;
import com.project.ridemesh.entities.NotificationTemplate;
import com.project.ridemesh.repositories.NotificationLogRepository;
import com.project.ridemesh.repositories.NotificationTemplateRepository;
import com.project.ridemesh.services.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final EmailSenderService emailSenderService;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(EmailSenderService emailSenderService,
                                NotificationTemplateRepository templateRepository,
                                NotificationLogRepository logRepository,
                                ObjectMapper objectMapper) {
        this.emailSenderService = emailSenderService;
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "ride-request-notification", groupId = "notify-service-group")
    public void consumeRideRequestNotification(String message) {
        try {
            log.info("Received ride-request-notification event: " + message);
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            String eventId = (String) payload.get("eventId");
            Long rideRequestId = ((Number) payload.get("rideRequestId")).longValue();
            List<String> driverEmails = (List<String>) payload.get("driverEmails");

            NotificationTemplate template = templateRepository.findByTemplateName("RIDE_REQUEST")
                    .orElseThrow(() -> new RuntimeException("RIDE_REQUEST template not seeded!"));

            String subject = template.getSubject();
            String bodyTemplate = template.getBody();
            String renderedBody = bodyTemplate.replace("{rideRequestId}", String.valueOf(rideRequestId));

            for (String email : driverEmails) {
                // Idempotency check: check if already sent for this eventId + email
                if (logRepository.findByEventIdAndRecipientEmail(eventId, email).isPresent()) {
                    log.info("Duplicate event detected for eventId: " + eventId + ", email: " + email + ". Skipping.");
                    continue;
                }

                boolean success = emailSenderService.sendEmail(email, subject, renderedBody);
                String status = success ? "SENT" : "FAILED";

                try {
                    NotificationLog notificationLog = new NotificationLog(eventId, email, subject, renderedBody, status);
                    logRepository.save(notificationLog);
                } catch (DataIntegrityViolationException dive) {
                    log.info("Duplicate concurrent event processing caught for eventId: " + eventId + ", email: " + email);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process ride-request-notification", e);
        }
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "ride-otp-notification", groupId = "notify-service-group")
    public void consumeRideOtpNotification(String message) {
        try {
            log.info("Received ride-otp-notification event: " + message);
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            String eventId = (String) payload.get("eventId");
            String email = (String) payload.get("email");
            String otp = (String) payload.get("otp");

            // Idempotency check
            if (logRepository.findByEventIdAndRecipientEmail(eventId, email).isPresent()) {
                log.info("Duplicate event detected for eventId: " + eventId + ", email: " + email + ". Skipping.");
                return;
            }

            NotificationTemplate template = templateRepository.findByTemplateName("RIDE_OTP")
                    .orElseThrow(() -> new RuntimeException("RIDE_OTP template not seeded!"));

            String subject = template.getSubject();
            String bodyTemplate = template.getBody();
            String renderedBody = bodyTemplate.replace("{otp}", otp);

            boolean success = emailSenderService.sendEmail(email, subject, renderedBody);
            String status = success ? "SENT" : "FAILED";

            try {
                NotificationLog notificationLog = new NotificationLog(eventId, email, subject, renderedBody, status);
                logRepository.save(notificationLog);
            } catch (DataIntegrityViolationException dive) {
                log.info("Duplicate concurrent event processing caught for eventId: " + eventId + ", email: " + email);
            }

        } catch (Exception e) {
            log.error("Failed to process ride-otp-notification", e);
        }
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "send-email-verification", groupId = "notify-service-group")
    public void consumeEmailVerification(String message) {
        log.info("Received send-email-verification event: " + message);
        try {
            Map<String, String> payload = objectMapper.readValue(message, Map.class);
            String email = payload.get("email");
            String verificationLink = payload.get("verificationLink");

            NotificationTemplate template = templateRepository.findByTemplateName("EMAIL_VERIFICATION")
                    .orElseThrow(() -> new RuntimeException("EMAIL_VERIFICATION template not seeded!"));

            String subject = template.getSubject();
            String bodyTemplate = template.getBody();
            String renderedBody = bodyTemplate.replace("{verificationLink}", verificationLink);

            String eventId = UUID.randomUUID().toString();

            boolean success = emailSenderService.sendEmail(email, subject, renderedBody);
            String status = success ? "SENT" : "FAILED";

            NotificationLog notificationLog = new NotificationLog(eventId, email, subject, renderedBody, status);
            logRepository.save(notificationLog);

        } catch (Exception e) {
            log.error("Failed to process send-email-verification", e);
        }
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "driver-application-notification", groupId = "notify-service-group")
    public void consumeDriverApplicationNotification(String message) {
        log.info("Received driver-application-notification event: " + message);
        try {
            Map<String, String> payload = objectMapper.readValue(message, Map.class);
            String email = payload.get("email");
            String subject = payload.get("subject");
            String body = payload.get("body");

            String eventId = UUID.randomUUID().toString();

            boolean success = emailSenderService.sendEmail(email, subject, body);
            String status = success ? "SENT" : "FAILED";

            NotificationLog notificationLog = new NotificationLog(eventId, email, subject, body, status);
            logRepository.save(notificationLog);
        } catch (Exception e) {
            log.error("Failed to process driver-application-notification", e);
        }
    }
}
