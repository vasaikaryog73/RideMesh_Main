package com.project.ridemesh.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.dto.DriverCancellationRequestDto;
import com.project.ridemesh.dto.DriverCancellationSubmitDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.DriverCancellationRequest;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.entities.enums.Roles;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.DriverApplicationRepository;
import com.project.ridemesh.repositories.DriverCancellationRequestRepository;
import com.project.ridemesh.repositories.DriverRepository;
import com.project.ridemesh.repositories.UserRepository;
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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
public class DriverCancellationController {

    private static final Logger log = LoggerFactory.getLogger(DriverCancellationController.class);

    private final DriverCancellationRequestRepository cancellationRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverApplicationRepository applicationRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Cache of OTPs per user ID: key=userId, value=OtpEntry
    private final Map<Long, OtpEntry> otpCache = new ConcurrentHashMap<>();

    private record OtpEntry(String otp, LocalDateTime expiry) {}

    public DriverCancellationController(DriverCancellationRequestRepository cancellationRepository,
                                        DriverRepository driverRepository,
                                        UserRepository userRepository,
                                        DriverApplicationRepository applicationRepository,
                                        ModelMapper modelMapper,
                                        KafkaTemplate<String, String> kafkaTemplate,
                                        ObjectMapper objectMapper) {
        this.cancellationRepository = cancellationRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send 6-digit OTP to driver's email for cancelling their account.
     */
    @PostMapping("/auth/driver-cancellation/send-otp")
    public ResponseEntity<Map<String, Object>> sendCancellationOtp() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpCache.put(currentUser.getId(), new OtpEntry(otp, LocalDateTime.now().plusMinutes(10)));

        String subject = "RideMesh - Driver Cancellation Verification Code";
        String body = "Hello " + currentUser.getName() + ",\n\n" +
                "Your One-Time Password (OTP) for requesting driver account cancellation is: " + otp + "\n\n" +
                "This verification code will expire in 10 minutes. Please enter this code in your browser to proceed with submitting your cancellation request.\n\n" +
                "If you did not request this, please secure your account immediately.\n\n" +
                "Best regards,\nRideMesh Team";

        sendNotificationEmail(currentUser.getEmail(), subject, body);

        log.info("Driver cancellation OTP generated and sent to {}: {}", currentUser.getEmail(), otp);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "A 6-digit verification code has been sent to " + currentUser.getEmail()
        ));
    }

    /**
     * Submit driver cancellation request after validating OTP.
     */
    @PostMapping("/auth/driver-cancellation/submit")
    public ResponseEntity<DriverCancellationRequestDto> submitCancellationRequest(@RequestBody DriverCancellationSubmitDto dto) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new RuntimeConflictException("Reason for cancellation is required.");
        }

        if (dto.getOtp() == null || dto.getOtp().trim().isEmpty()) {
            throw new RuntimeConflictException("Verification OTP is required.");
        }

        OtpEntry entry = otpCache.get(currentUser.getId());
        if (entry == null) {
            throw new RuntimeConflictException("No active OTP found. Please request a new verification code.");
        }

        if (entry.expiry().isBefore(LocalDateTime.now())) {
            otpCache.remove(currentUser.getId());
            throw new RuntimeConflictException("OTP has expired. Please request a new verification code.");
        }

        if (!entry.otp().equals(dto.getOtp().trim())) {
            throw new RuntimeConflictException("Invalid OTP code. Please enter the correct 6-digit code.");
        }

        // OTP verified successfully, clear it
        otpCache.remove(currentUser.getId());

        Driver driver = driverRepository.findByUser(currentUser)
                .orElse(null);

        DriverCancellationRequest request = new DriverCancellationRequest();
        request.setUserId(currentUser.getId());
        if (driver != null) {
            request.setDriverId(driver.getId());
            request.setVehicleId(driver.getVehicleId());
            request.setVehicleType(driver.getVehicleType());
            request.setPhoneNumber(driver.getPhoneNumber() != null ? driver.getPhoneNumber() : currentUser.getPhoneNumber());
        } else {
            request.setPhoneNumber(currentUser.getPhoneNumber());
            request.setVehicleId("N/A");
            request.setVehicleType("N/A");
        }
        request.setDriverName(currentUser.getName());
        request.setDriverEmail(currentUser.getEmail());
        request.setReason(dto.getReason().trim());
        request.setMediaUrl(dto.getMediaUrl());
        request.setOtp(dto.getOtp().trim());
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());

        DriverCancellationRequest saved = cancellationRepository.save(request);

        String subject = "RideMesh - Driver Cancellation Request Received";
        String body = "Hello " + currentUser.getName() + ",\n\n" +
                "We have received your driver account cancellation request.\n\n" +
                "Submitted Reason:\n" + dto.getReason().trim() + "\n\n" +
                "Our administrative team will review your application and process the cancellation shortly. You will be notified once reviewed.\n\n" +
                "RideMesh Support Team";

        sendNotificationEmail(currentUser.getEmail(), subject, body);

        return ResponseEntity.ok(modelMapper.map(saved, DriverCancellationRequestDto.class));
    }

    /**
     * List all cancellation requests for Admin.
     */
    @GetMapping("/auth/driver-cancellation/requests")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<DriverCancellationRequestDto>> getCancellationRequests() {
        List<DriverCancellationRequestDto> list = cancellationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(req -> modelMapper.map(req, DriverCancellationRequestDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * Admin approves driver cancellation:
     * Removes DRIVER role from user, deletes Driver record, reverts user to RIDER.
     */
    @PostMapping("/auth/driver-cancellation/requests/{id}/approve")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DriverCancellationRequestDto> approveCancellation(@PathVariable Long id) {
        DriverCancellationRequest request = cancellationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation request not found: " + id));

        request.setStatus("APPROVED");
        request.setResolvedAt(LocalDateTime.now());
        DriverCancellationRequest saved = cancellationRepository.save(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        // Remove DRIVER role
        user.getRoles().remove(Roles.DRIVER);
        if (user.getRoles().isEmpty() || !user.getRoles().contains(Roles.RIDER)) {
            user.getRoles().add(Roles.RIDER);
        }
        userRepository.save(user);

        // Delete Driver record
        driverRepository.findByUser(user).ifPresent(driverRepository::delete);

        // Mark any previous approved driver application as CANCELLED
        applicationRepository.findByUserId(user.getId()).forEach(app -> {
            if ("APPROVED".equals(app.getStatus())) {
                app.setStatus("CANCELLED");
                applicationRepository.save(app);
            }
        });

        String subject = "RideMesh - Driver Account Cancellation Approved";
        String body = "Hello " + user.getName() + ",\n\n" +
                "Your request to cancel your driver account has been approved by the RideMesh Admin team.\n\n" +
                "Your driver account has been deactivated and your profile has been reverted to Rider status. " +
                "You can continue to use RideMesh as a passenger to book rides whenever you need.\n\n" +
                "If you wish to drive with RideMesh again in the future, you are welcome to submit a new application.\n\n" +
                "Warm regards,\nRideMesh Team";

        sendNotificationEmail(user.getEmail(), subject, body);

        return ResponseEntity.ok(modelMapper.map(saved, DriverCancellationRequestDto.class));
    }

    /**
     * Admin rejects driver cancellation.
     */
    @PostMapping("/auth/driver-cancellation/requests/{id}/reject")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DriverCancellationRequestDto> rejectCancellation(@PathVariable Long id) {
        DriverCancellationRequest request = cancellationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation request not found: " + id));

        request.setStatus("REJECTED");
        request.setResolvedAt(LocalDateTime.now());
        DriverCancellationRequest saved = cancellationRepository.save(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        String subject = "RideMesh - Driver Account Cancellation Request Update";
        String body = "Hello " + user.getName() + ",\n\n" +
                "Your request to cancel your driver account has been declined by the RideMesh Admin team.\n\n" +
                "If you have questions or require further assistance, please contact us at support@ridemesh.com.\n\n" +
                "Warm regards,\nRideMesh Team";

        sendNotificationEmail(user.getEmail(), subject, body);

        return ResponseEntity.ok(modelMapper.map(saved, DriverCancellationRequestDto.class));
    }

    /**
     * Toggle driver availability between Online and Offline.
     */
    @PostMapping({"/auth/driver/toggle-availability", "/driver/toggle-availability"})
    public ResponseEntity<Map<String, Object>> toggleAvailability() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("No driver profile found for user ID: " + currentUser.getId()));

        boolean currentStatus = Boolean.TRUE.equals(driver.getAvailable());
        boolean newStatus = !currentStatus;
        driver.setAvailable(newStatus);
        Driver saved = driverRepository.save(driver);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "available", saved.getAvailable()
        ));
    }

    private void sendNotificationEmail(String email, String subject, String body) {
        try {
            Map<String, String> payload = Map.of(
                    "email", email,
                    "subject", subject,
                    "body", body
            );
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("driver-application-notification", json);
        } catch (Exception e) {
            log.error("Failed to publish driver-application-notification to Kafka", e);
        }
    }
}
