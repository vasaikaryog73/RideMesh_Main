package com.project.ridemesh.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.advices.ApiResponse;
import com.project.ridemesh.dto.DriverReportDto;
import com.project.ridemesh.dto.DriverReportSubmitDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.DriverReport;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.entities.enums.Roles;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.DriverReportRepository;
import com.project.ridemesh.repositories.DriverRepository;
import com.project.ridemesh.repositories.UserRepository;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/driver-reports")
public class DriverReportController {

    private static final Logger log = LoggerFactory.getLogger(DriverReportController.class);

    private final DriverReportRepository reportRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DriverReportController(DriverReportRepository reportRepository,
                                  DriverRepository driverRepository,
                                  UserRepository userRepository,
                                  ModelMapper modelMapper,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Submit a driver report (Rider action).
     * Compulsory reason and media evidence.
     * Mails rider confirmation upon receipt.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DriverReportDto>> submitReport(@Valid @RequestBody DriverReportSubmitDto submitDto) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (submitDto.getMediaUrl() == null || submitDto.getMediaUrl().trim().isEmpty()) {
            throw new RuntimeConflictException("Evidence media is compulsory to report a driver.");
        }
        if (submitDto.getReason() == null || submitDto.getReason().trim().isEmpty()) {
            throw new RuntimeConflictException("Reason is required to report a driver.");
        }

        Driver driver = driverRepository.findById(submitDto.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + submitDto.getDriverId()));

        DriverReport report = new DriverReport();
        report.setRideId(submitDto.getRideId());
        report.setRiderId(currentUser.getId());
        report.setRiderName(currentUser.getName());
        report.setRiderEmail(currentUser.getEmail());

        report.setDriverId(driver.getId());
        if (driver.getUser() != null) {
            report.setDriverName(driver.getUser().getName());
            report.setDriverEmail(driver.getUser().getEmail());
            report.setDriverUserId(driver.getUser().getId());
        }
        report.setReason(submitDto.getReason());
        report.setMediaUrl(submitDto.getMediaUrl());
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());

        DriverReport saved = reportRepository.save(report);

        // Mail to rider as requested:
        // "Your report was recieved, we will look into this matter. thanks for reporting, this helps keep our plaatform safe"
        String riderEmail = currentUser.getEmail();
        String subject = "RideMesh - Your Report Was Received";
        String body = "Your report was recieved, we will look into this matter. thanks for reporting, this helps keep our plaatform safe";
        sendEmail(riderEmail, subject, body);

        DriverReportDto responseDto = modelMapper.map(saved, DriverReportDto.class);
        return ResponseEntity.ok(new ApiResponse<>(responseDto));
    }

    /**
     * Fetch all driver reports for Admin.
     */
    @GetMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<List<DriverReportDto>>> getAllReports() {
        List<DriverReportDto> dtos = reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(r -> modelMapper.map(r, DriverReportDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(dtos));
    }

    /**
     * Admin action: Send a notice to the driver.
     */
    @PostMapping("/{reportId}/send-notice")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<DriverReportDto>> sendNotice(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> payload) {
        DriverReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        String noticeMessage = payload.get("noticeMessage");
        if (noticeMessage == null || noticeMessage.trim().isEmpty()) {
            throw new RuntimeConflictException("Notice message cannot be empty.");
        }

        report.setNoticeMessage(noticeMessage);
        report.setStatus("NOTICE_SENT");
        report.setUpdatedAt(LocalDateTime.now());
        DriverReport saved = reportRepository.save(report);

        if (report.getDriverEmail() != null) {
            String subject = "Important Notice Regarding Your RideMesh Driver Account";
            String body = "Hello " + (report.getDriverName() != null ? report.getDriverName() : "Driver") + ",\n\n"
                    + "An official notice has been issued regarding an incident report submitted against you:\n\n"
                    + "\"" + noticeMessage + "\"\n\n"
                    + "Please maintain safety and compliance standards. Multiple notices or safety violations may lead to immediate account suspension.\n\n"
                    + "Warm regards,\nRideMesh Administration Team";
            sendEmail(report.getDriverEmail(), subject, body);
        }

        return ResponseEntity.ok(new ApiResponse<>(modelMapper.map(saved, DriverReportDto.class)));
    }

    /**
     * Admin action: Suspend the driver directly from the report application.
     */
    @PostMapping("/{reportId}/suspend-driver")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<DriverReportDto>> suspendDriverFromReport(@PathVariable Long reportId) {
        DriverReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        Long driverUserId = report.getDriverUserId();
        if (driverUserId != null) {
            userRepository.findById(driverUserId).ifPresent(user -> {
                user.getRoles().clear();
                user.getRoles().add(Roles.RIDER);
                userRepository.save(user);

                driverRepository.findByUser(user).ifPresent(driverRepository::delete);

                sendEmail(
                        user.getEmail(),
                        "RideMesh Driver Account Suspended",
                        "Your driver account has been suspended following a review of incident reports. You have been reverted to Rider status."
                );
            });
        }

        report.setStatus("SUSPENDED");
        report.setUpdatedAt(LocalDateTime.now());
        DriverReport saved = reportRepository.save(report);

        return ResponseEntity.ok(new ApiResponse<>(modelMapper.map(saved, DriverReportDto.class)));
    }

    private void sendEmail(String email, String subject, String body) {
        try {
            Map<String, String> payload = Map.of(
                    "email", email,
                    "subject", subject,
                    "body", body
            );
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("driver-application-notification", json);
            log.info("Dispatched email notification via Kafka to: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish email notification to Kafka for {}", email, e);
        }
    }
}
