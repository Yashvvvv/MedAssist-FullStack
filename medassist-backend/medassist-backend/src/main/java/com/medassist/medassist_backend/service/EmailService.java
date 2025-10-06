package com.medassist.medassist_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

@Service
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true", matchIfMissing = true)
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${medassist.app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String to, String username, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            Context context = new Context(Locale.getDefault());
            context.setVariable("username", username);
            context.setVariable("verificationUrl", baseUrl + "/api/auth/verify-email?token=" + token);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/verification", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("MedAssist - Email Verification Required");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Fallback to simple email
            sendSimpleVerificationEmail(to, username, token);
        }
    }

    public void sendPasswordResetEmail(String to, String username, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            Context context = new Context(Locale.getDefault());
            context.setVariable("username", username);
            context.setVariable("resetUrl", baseUrl + "/api/auth/reset-password?token=" + token);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/password-reset", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("MedAssist - Password Reset Request");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Fallback to simple email
            sendSimplePasswordResetEmail(to, username, token);
        }
    }

    public void sendHealthcareProviderVerificationEmail(String to, String username, String licenseNumber) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            Context context = new Context(Locale.getDefault());
            context.setVariable("username", username);
            context.setVariable("licenseNumber", licenseNumber);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/healthcare-verification", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("MedAssist - Healthcare Provider Verification Submitted");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Fallback to simple email
            sendSimpleHealthcareProviderEmail(to, username, licenseNumber);
        }
    }

    private void sendSimpleVerificationEmail(String to, String username, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromEmail);
        message.setSubject("MedAssist - Email Verification Required");
        message.setText("Hello " + username + ",\n\n" +
                "Please verify your email address by clicking the link below:\n" +
                baseUrl + "/api/auth/verify-email?token=" + token + "\n\n" +
                "If you did not create this account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "MedAssist Team");

        mailSender.send(message);
    }

    private void sendSimplePasswordResetEmail(String to, String username, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromEmail);
        message.setSubject("MedAssist - Password Reset Request");
        message.setText("Hello " + username + ",\n\n" +
                "You have requested to reset your password. Please click the link below to reset it:\n" +
                baseUrl + "/api/auth/reset-password?token=" + token + "\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "MedAssist Team");

        mailSender.send(message);
    }

    private void sendSimpleHealthcareProviderEmail(String to, String username, String licenseNumber) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromEmail);
        message.setSubject("MedAssist - Healthcare Provider Verification Submitted");
        message.setText("Hello " + username + ",\n\n" +
                "Your healthcare provider verification has been submitted with license number: " + licenseNumber + "\n\n" +
                "Our team will review your credentials and notify you once verified.\n\n" +
                "Best regards,\n" +
                "MedAssist Team");

        mailSender.send(message);
    }
}
