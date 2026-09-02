package com.project.ridemesh.services.implementations;

import com.project.ridemesh.services.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderServiceImpl.class);
    private final JavaMailSender mailSender;

    public EmailSenderServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendEmail(String toEmail, String subject, String body) {
        log.info("---------------- EMAIL DISPATCH ----------------");
        log.info("To: " + toEmail);
        log.info("Subject: " + subject);
        log.info("Body:\n" + body);
        log.info("------------------------------------------------");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("SMTP Email sent successfully!");
            return true;
        } catch (Exception e) {
            log.warn("SMTP Mail dispatch failed (using fallback logging). Error: " + e.getMessage());
            return false;
        }
    }
}
