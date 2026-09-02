package com.project.ridemesh.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(indexes = {
        @Index(name = "idx_notification_template_name", columnList = "templateName", unique = true)
})
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Template name is required")
    @Column(unique = true, nullable = false)
    private String templateName;

    @NotNull(message = "Subject is required")
    private String subject;

    @NotNull(message = "Body is required")
    @Column(length = 2000)
    private String body;

    public NotificationTemplate() {
    }

    public NotificationTemplate(String templateName, String subject, String body) {
        this.templateName = templateName;
        this.subject = subject;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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
}
