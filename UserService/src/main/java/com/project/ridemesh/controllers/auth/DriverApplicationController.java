package com.project.ridemesh.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.dto.DriverApplicationDto;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.dto.OnboardDriverDto;
import com.project.ridemesh.dto.PointDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.DriverApplication;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.entities.enums.Roles;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.DriverApplicationRepository;
import com.project.ridemesh.repositories.DriverRepository;
import com.project.ridemesh.repositories.UserRepository;
import com.project.ridemesh.services.AuthService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.project.ridemesh.advices.ApiError;
import com.project.ridemesh.advices.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class DriverApplicationController {

    private final DriverApplicationRepository applicationRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final com.project.ridemesh.repositories.DriverReportRepository driverReportRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AuthService authService;

    public DriverApplicationController(DriverApplicationRepository applicationRepository,
                                       DriverRepository driverRepository,
                                       UserRepository userRepository,
                                       com.project.ridemesh.repositories.DriverReportRepository driverReportRepository,
                                       ModelMapper modelMapper,
                                       KafkaTemplate<String, String> kafkaTemplate,
                                       ObjectMapper objectMapper,
                                       AuthService authService) {
        this.applicationRepository = applicationRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.driverReportRepository = driverReportRepository;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.authService = authService;
    }

    @PostMapping("/driver-applications")
    @Secured("ROLE_RIDER")
    public ResponseEntity<DriverApplicationDto> submitApplication(@RequestBody DriverApplicationDto dto) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().trim().isEmpty()) {
            String registeredDigits = currentUser.getPhoneNumber().replaceAll("[^0-9]", "");
            String submittedDigits = dto.getPhoneNumber() != null ? dto.getPhoneNumber().replaceAll("[^0-9]", "") : "";
            boolean matches = false;
            if (registeredDigits.length() >= 10 && submittedDigits.length() >= 10) {
                String reg10 = registeredDigits.substring(registeredDigits.length() - 10);
                String sub10 = submittedDigits.substring(submittedDigits.length() - 10);
                matches = reg10.equals(sub10);
            } else {
                matches = registeredDigits.equals(submittedDigits);
            }
            if (!matches) {
                throw new com.project.ridemesh.exceptions.RuntimeConflictException(
                    "Mobile number entered (" + dto.getPhoneNumber() + ") must match your registered phone number (" + currentUser.getPhoneNumber() + ")."
                );
            }
        }

        DriverApplication app = new DriverApplication();
        app.setUserId(currentUser.getId());
        app.setRiderName(currentUser.getName());
        app.setVehicleModel(dto.getVehicleModel());
        app.setVehicleId(dto.getVehicleId());
        app.setLicenseNumber(dto.getLicenseNumber());
        app.setLicensePhotoUrl(dto.getLicensePhotoUrl());
        app.setVehicleType(dto.getVehicleType());
        app.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getLongitude() != null && dto.getLatitude() != null) {
            app.setLongitude(dto.getLongitude());
            app.setLatitude(dto.getLatitude());
        } else {
            double[] coords = getDistrictCenter(dto.getDistrict());
            app.setLongitude(coords[0]);
            app.setLatitude(coords[1]);
        }
        app.setState(dto.getState());
        app.setDistrict(dto.getDistrict());
        app.setVehiclePhotos(dto.getVehiclePhotos());
        app.setIdentityPhotoUrl(dto.getIdentityPhotoUrl());
        app.setIdentityDocType(dto.getIdentityDocType());
        app.setStatus("PENDING");
        
        DriverApplication saved = applicationRepository.save(app);

        sendNotificationEmail(
            currentUser.getEmail(),
            "Driver Application Received",
            "We've successfully received your application of becoming a driver. We'll go through and verify your submitted details and documents and will inform you shortly."
        );

        return ResponseEntity.ok(modelMapper.map(saved, DriverApplicationDto.class));
    }

    @GetMapping("/driver-applications/my-application")
    public ResponseEntity<DriverApplicationDto> getMyApplication(@RequestParam(required = false) Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User) {
                targetUserId = ((User) auth.getPrincipal()).getId();
            }
        }
        if (targetUserId == null) {
            return ResponseEntity.ok(new DriverApplicationDto());
        }
        return applicationRepository.findTopByUserIdOrderByIdDesc(targetUserId)
                .map(app -> ResponseEntity.ok(modelMapper.map(app, DriverApplicationDto.class)))
                .orElse(ResponseEntity.ok(new DriverApplicationDto()));
    }

    @GetMapping("/driver-applications")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<DriverApplicationDto>> getApplications() {
        List<DriverApplicationDto> list = applicationRepository.findAll()
                .stream()
                .map(app -> modelMapper.map(app, DriverApplicationDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/driver-applications/{id}/approve")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DriverApplicationDto> approveApplication(@PathVariable Long id) {
        DriverApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        app.setStatus("APPROVED");
        DriverApplication saved = applicationRepository.save(app);

        // Automatically onboard the driver!
        OnboardDriverDto onboardDto = new OnboardDriverDto();
        onboardDto.setVehicleId(app.getVehicleId());
        onboardDto.setVehicleType(app.getVehicleType());
        onboardDto.setPhoneNumber(app.getPhoneNumber());
        onboardDto.setState(app.getState());
        onboardDto.setDistrict(app.getDistrict());
        double[] coords = getDistrictCenter(app.getDistrict());
        double lon = coords[0];
        double lat = coords[1];
        onboardDto.setCurrentLocation(new PointDto(new double[]{lon, lat}));
        authService.onboardNewDriver(app.getUserId(), onboardDto);

        User user = userRepository.findById(app.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + app.getUserId()));

        sendNotificationEmail(
            user.getEmail(),
            "Driver Application Approved!",
            "Congratulations! your request to become a driver has approved by our team! we will update your profile soon."
        );

        return ResponseEntity.ok(modelMapper.map(saved, DriverApplicationDto.class));
    }

    @PostMapping("/driver-applications/{id}/reject")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DriverApplicationDto> rejectApplication(@PathVariable Long id) {
        DriverApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        app.setStatus("REJECTED");
        DriverApplication saved = applicationRepository.save(app);

        User user = userRepository.findById(app.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + app.getUserId()));

        sendNotificationEmail(
            user.getEmail(),
            "Driver Application Update",
            "We are sorry to inform you that your request to become a driver has been rejected by our team. If you think rejection is by mistake, you can apply again or contact us on our email suport@ridemesh.com"
        );

        return ResponseEntity.ok(modelMapper.map(saved, DriverApplicationDto.class));
    }

    @GetMapping("/drivers")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<DriverDto>> getDrivers() {
        List<DriverDto> list = driverRepository.findAll()
                .stream()
                .map(driver -> {
                    DriverDto dto = modelMapper.map(driver, DriverDto.class);
                    applicationRepository.findByUserId(driver.getUser().getId())
                            .stream()
                            .filter(app -> "APPROVED".equals(app.getStatus()))
                            .findFirst()
                            .ifPresent(app -> {
                                dto.setLicenseNumber(app.getLicenseNumber());
                                dto.setLicensePhotoUrl(app.getLicensePhotoUrl());
                                dto.setVehiclePhotos(app.getVehiclePhotos());
                                dto.setIdentityPhotoUrl(app.getIdentityPhotoUrl());
                                dto.setIdentityDocType(app.getIdentityDocType());
                            });
                    long reportCount = driverReportRepository.countByDriverId(driver.getId());
                    dto.setReportsCount(reportCount);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/drivers/{userId}/suspend")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> suspendDriver(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            
            user.getRoles().clear();
            user.getRoles().add(Roles.RIDER);
            userRepository.save(user);

            driverRepository.findByUser(user).ifPresent(driverRepository::delete);

            sendNotificationEmail(
                user.getEmail(),
                "Driver Account Suspended",
                "Sorry to inform you that you are no longer a Driver. You can still reapply for Becoming a Driver."
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiError apiError = new ApiError.ApiErrorBuilder()
                .setStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .setMessage("SuspendError: " + e.getClass().getName() + " - " + e.getMessage())
                .build();
            return new ResponseEntity<>(new ApiResponse<>(apiError), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            // Ignore
        }
    }

    private double[] getDistrictCenter(String district) {
        if (district == null) return new double[]{72.8600, 19.0400};
        String d = district.toLowerCase().trim();
        switch (d) {
            case "mumbai suburban":
                return new double[]{72.8368, 19.1076}; // Vile Parle / Cooper Hospital range center
            case "mumbai":
                return new double[]{72.8258, 18.9750};
            case "thane":
                return new double[]{72.9781, 19.2183};
            case "palghar":
                return new double[]{72.7656, 19.6967};
            case "pune":
                return new double[]{73.8567, 18.5204};
            case "nashik":
                return new double[]{73.7898, 19.9975};
            case "bengaluru urban":
                return new double[]{77.5946, 12.9716};
            case "bengaluru rural":
                return new double[]{77.5750, 13.2847};
            case "kolar":
                return new double[]{78.1292, 13.1368};
            case "tumakuru":
                return new double[]{77.1006, 13.3409};
            case "new delhi":
            case "south delhi":
            case "central delhi":
            case "south west delhi":
            case "west delhi":
            case "east delhi":
                return new double[]{77.2090, 28.6139};
            default:
                return new double[]{72.8600, 19.0400};
        }
    }
}
