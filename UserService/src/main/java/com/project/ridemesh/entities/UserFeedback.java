package com.project.ridemesh.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_feedback")
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String userName;
    private String userEmail;
    private String userRole; // RIDER, DRIVER, etc.

    @Column(nullable = false)
    private String type; // FEEDBACK, SUGGESTION, COMPLAINT, BUG_REPORT

    private String subType; // For FEEDBACK: POSITIVE, NEGATIVE; For COMPLAINT: AGAINST_DRIVER, AGAINST_PLATFORM

    @Column(length = 4000, nullable = false)
    private String content;

    @Column(length = 1000)
    private String mediaUrl; // Compulsory if BUG_REPORT, optional for others

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, REPLIED, RESOLVED

    @Column(length = 4000)
    private String adminReply;

    private LocalDateTime adminRepliedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UserFeedback() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public LocalDateTime getAdminRepliedAt() {
        return adminRepliedAt;
    }

    public void setAdminRepliedAt(LocalDateTime adminRepliedAt) {
        this.adminRepliedAt = adminRepliedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
