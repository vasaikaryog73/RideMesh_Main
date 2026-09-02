package com.project.ridemesh.config;

import com.project.ridemesh.entities.NotificationTemplate;
import com.project.ridemesh.repositories.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TemplateDatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TemplateDatabaseSeeder.class);
    private final NotificationTemplateRepository templateRepository;

    public TemplateDatabaseSeeder(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (templateRepository.findByTemplateName("RIDE_REQUEST").isEmpty()) {
            NotificationTemplate requestTemplate = new NotificationTemplate(
                    "RIDE_REQUEST",
                    "New Ride Request Alert!",
                    "Hello,\n\nA new ride request is available nearby. Request ID: {rideRequestId}\n\nPlease log in to accept the ride!\n\nThank you!"
            );
            templateRepository.save(requestTemplate);
            log.info("Seeded default RIDE_REQUEST notification template.");
        }

        if (templateRepository.findByTemplateName("RIDE_OTP").isEmpty()) {
            NotificationTemplate otpTemplate = new NotificationTemplate(
                    "RIDE_OTP",
                    "Your RideMesh Verification OTP",
                    "Hello,\n\nYour ride verification OTP is: {otp}\n\nPlease share this with the driver to start the ride.\n\nThank you!"
            );
            templateRepository.save(otpTemplate);
            log.info("Seeded default RIDE_OTP notification template.");
        }

        if (templateRepository.findByTemplateName("EMAIL_VERIFICATION").isEmpty()) {
            NotificationTemplate verificationTemplate = new NotificationTemplate(
                    "EMAIL_VERIFICATION",
                    "Verify Your RideMesh Account",
                    "Hello,\n\nPlease verify your email by clicking the link below:\n\n{verificationLink}\n\nIf you did not request this, please ignore this email.\n\nThank you!"
            );
            templateRepository.save(verificationTemplate);
            log.info("Seeded default EMAIL_VERIFICATION notification template.");
        }
    }
}
