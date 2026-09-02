package com.project.ridemesh.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.entities.NotificationLog;
import com.project.ridemesh.entities.NotificationTemplate;
import com.project.ridemesh.repositories.NotificationLogRepository;
import com.project.ridemesh.repositories.NotificationTemplateRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/test-notification")
public class TestNotificationController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public TestNotificationController(KafkaTemplate<String, String> kafkaTemplate,
                                      NotificationLogRepository logRepository,
                                      NotificationTemplateRepository templateRepository,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/trigger-otp")
    public String triggerOtp(@RequestParam String email, @RequestParam String otp) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("email", email);
        payload.put("otp", otp);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        kafkaTemplate.send("ride-otp-notification", jsonPayload);
        return "Published mock OTP event to Kafka for: " + email;
    }

    @PostMapping("/trigger-request")
    public String triggerRequest(@RequestParam Long rideRequestId, @RequestBody List<String> driverEmails) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("rideRequestId", rideRequestId);
        payload.put("driverEmails", driverEmails);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        kafkaTemplate.send("ride-request-notification", jsonPayload);
        return "Published mock Ride Request event to Kafka for Request ID: " + rideRequestId;
    }

    @GetMapping("/logs")
    public List<NotificationLog> getAllLogs() {
        return logRepository.findAll();
    }

    @GetMapping("/templates")
    public List<NotificationTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }
}
