package com.project.ridemesh.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notification_log",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_event_recipient", columnNames = {"eventId", "recipientEmail"})
    },
    indexes = {
        @Index(name = "idx_notification_log_event_recipient", columnList = "eventId,recipientEmail")
    }
)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Event ID is required")
    private String eventId;

    @NotNull(message = "Recipient email is required")
    private String recipientEmail;

    private String subject;

    @Column(length = 2000)
    private String body;

    @NotNull(message = "Status is required")
    private String status; // SENT, FAILED

    @CreationTimestamp
    private LocalDateTime timestamp;

    public NotificationLog() {
    }

    public NotificationLog(String eventId, String recipientEmail, String subject, String body, String status) {
        this.eventId = eventId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
