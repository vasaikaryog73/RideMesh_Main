package com.project.ridemesh.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ridemesh.advices.ApiResponse;
import com.project.ridemesh.dto.UserFeedbackDto;
import com.project.ridemesh.dto.UserFeedbackSubmitDto;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.entities.UserFeedback;
import com.project.ridemesh.entities.enums.Roles;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.UserFeedbackRepository;
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
@RequestMapping("/auth/feedback")
public class UserFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(UserFeedbackController.class);

    private final UserFeedbackRepository feedbackRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserFeedbackController(UserFeedbackRepository feedbackRepository,
                                  ModelMapper modelMapper,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.feedbackRepository = feedbackRepository;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Submit feedback/suggestion/complaint/bug report by any authenticated user (Rider/Driver).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserFeedbackDto>> submitFeedback(@Valid @RequestBody UserFeedbackSubmitDto submitDto) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String type = submitDto.getType() != null ? submitDto.getType().trim().toUpperCase() : "";
        if (type.isEmpty()) {
            throw new RuntimeConflictException("Category (Feedback, Suggestion, Complaint, or Report a Bug) is required.");
        }

        if (submitDto.getContent() == null || submitDto.getContent().trim().isEmpty()) {
            throw new RuntimeConflictException("Content cannot be empty.");
        }

        // If REPORT_BUG or BUG_REPORT, media is compulsory
        if (("BUG_REPORT".equals(type) || "REPORT_BUG".equals(type)) &&
                (submitDto.getMediaUrl() == null || submitDto.getMediaUrl().trim().isEmpty())) {
            throw new RuntimeConflictException("Compulsory evidence (media) is required when reporting a bug.");
        }

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(currentUser.getId());
        feedback.setUserName(currentUser.getName());
        feedback.setUserEmail(currentUser.getEmail());

        String role = currentUser.getRoles() != null && currentUser.getRoles().contains(Roles.DRIVER)
                ? "DRIVER" : "RIDER";
        feedback.setUserRole(role);

        feedback.setType(type);
        feedback.setSubType(submitDto.getSubType());
        feedback.setContent(submitDto.getContent().trim());
        feedback.setMediaUrl(submitDto.getMediaUrl());
        feedback.setStatus("PENDING");
        feedback.setCreatedAt(LocalDateTime.now());

        UserFeedback saved = feedbackRepository.save(feedback);

        UserFeedbackDto dto = modelMapper.map(saved, UserFeedbackDto.class);
        return ResponseEntity.ok(new ApiResponse<>(dto));
    }

    /**
     * Fetch all items for Admin Improvement Console.
     */
    @GetMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<List<UserFeedbackDto>>> getAllFeedback(
            @RequestParam(value = "type", required = false) String type) {
        List<UserFeedback> list;
        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            list = feedbackRepository.findByTypeOrderByCreatedAtDesc(type.trim().toUpperCase());
        } else {
            list = feedbackRepository.findAllByOrderByCreatedAtDesc();
        }

        List<UserFeedbackDto> dtos = list.stream()
                .map(item -> modelMapper.map(item, UserFeedbackDto.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(dtos));
    }

    /**
     * Admin replies to a feedback/complaint/suggestion/bug item.
     * Dispatches an email to the submitter via Kafka notification topic.
     */
    @PostMapping("/{id}/reply")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<UserFeedbackDto>> replyToFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        UserFeedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));

        String replyMessage = payload.get("reply");
        if (replyMessage == null || replyMessage.trim().isEmpty()) {
            throw new RuntimeConflictException("Reply message cannot be empty.");
        }

        feedback.setAdminReply(replyMessage.trim());
        feedback.setAdminRepliedAt(LocalDateTime.now());
        feedback.setStatus("REPLIED");
        UserFeedback saved = feedbackRepository.save(feedback);

        // Send email to sender
        if (feedback.getUserEmail() != null && !feedback.getUserEmail().trim().isEmpty()) {
            String subject = "RideMesh Update: Regarding your " + formatCategoryName(feedback.getType());
            String body = "Hello " + (feedback.getUserName() != null ? feedback.getUserName() : "User") + ",\n\n"
                    + "Thank you for reaching out to RideMesh Improvement Team regarding your " + formatCategoryName(feedback.getType()) + ".\n\n"
                    + "Our Response:\n"
                    + replyMessage.trim() + "\n\n"
                    + "Best regards,\nRideMesh Team";
            sendEmail(feedback.getUserEmail(), subject, body);
        }

        return ResponseEntity.ok(new ApiResponse<>(modelMapper.map(saved, UserFeedbackDto.class)));
    }

    private String formatCategoryName(String type) {
        if ("FEEDBACK".equalsIgnoreCase(type)) return "Feedback";
        if ("SUGGESTION".equalsIgnoreCase(type)) return "Suggestion";
        if ("COMPLAINT".equalsIgnoreCase(type)) return "Complaint";
        if ("BUG_REPORT".equalsIgnoreCase(type) || "REPORT_BUG".equalsIgnoreCase(type)) return "Bug Report";
        return "Message";
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
            log.info("Dispatched Improvement Console reply email via Kafka to: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish feedback reply email to Kafka for {}", email, e);
        }
    }
}
