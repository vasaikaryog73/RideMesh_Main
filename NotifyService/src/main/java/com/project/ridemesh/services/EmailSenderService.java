package com.project.ridemesh.services;

public interface EmailSenderService {
    boolean sendEmail(String toEmail, String subject, String body);
}
