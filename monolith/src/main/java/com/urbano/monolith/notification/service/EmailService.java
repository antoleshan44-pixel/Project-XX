package com.urbano.monolith.notification.service;

import com.urbano.monolith.notification.dto.NotificationRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.name:Urbano Homes}")
    private String appName;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * Send a simple text email
     */
    public void sendTextEmail(String to, String subject, String content) {
        // Check if mail is enabled
        if (!mailEnabled) {
            log.info("Email is disabled. Would send to '{}' with subject '{}'", to, subject);
            return;
        }

        // Validate from email
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("No 'from' email configured. Would send to '{}' with subject '{}'", to, subject);
            return;
        }

        // Validate recipient
        if (to == null || to.trim().isEmpty()) {
            log.error("Cannot send email: recipient is empty");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.trim());
            message.setTo(to.trim());
            message.setSubject(subject != null ? subject : "Notification from " + appName);
            message.setText(content != null ? content : "No content provided");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw - email failures should not break the flow
        }
    }

    /**
     * Send an HTML email with template
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        // Check if mail is enabled
        if (!mailEnabled) {
            log.info("Email is disabled. Would send HTML email to '{}' with subject '{}'", to, subject);
            return;
        }

        // Validate from email
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("No 'from' email configured. Would send HTML email to '{}'", to);
            return;
        }

        // Validate recipient
        if (to == null || to.trim().isEmpty()) {
            log.error("Cannot send HTML email: recipient is empty");
            return;
        }

        try {
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail.trim());
            helper.setTo(to.trim());
            helper.setSubject(subject != null ? subject : "Notification from " + appName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            // Don't throw - email failures should not break the flow
        } catch (Exception e) {
            log.error("Unexpected error sending HTML email to {}: {}", to, e.getMessage());
            // Don't throw - email failures should not break the flow
        }
    }

    /**
     * Send a notification email based on request
     */
    public void sendNotificationEmail(NotificationRequest request) {
        if (request == null) {
            log.warn("Cannot send notification email: request is null");
            return;
        }

        String to = request.getRecipient();
        String subject = request.getSubject() != null ? request.getSubject() :
                generateSubject(request.getType());
        String content = request.getContent() != null ? request.getContent() :
                generateContent(request);

        // If template is provided, use HTML email
        if (request.getTemplateName() != null && request.getTemplateData() != null) {
            Context context = new Context();
            request.getTemplateData().forEach(context::setVariable);
            context.setVariable("appName", appName);
            sendHtmlEmail(to, subject, request.getTemplateName(), context);
        } else {
            sendTextEmail(to, subject, content);
        }
    }

    private String generateSubject(String type) {
        if (type == null) {
            return "Notification from " + appName;
        }
        return switch (type) {
            case "RENT_REMINDER" -> "Rent Reminder - " + appName;
            case "LEASE_CONFIRMATION" -> "Lease Confirmation - " + appName;
            case "MAINTENANCE_UPDATE" -> "Maintenance Request Update - " + appName;
            case "PAYMENT_RECEIVED" -> "Payment Received - " + appName;
            case "TENANT_INVITE" -> "Welcome to " + appName;
            default -> "Notification from " + appName;
        };
    }

    private String generateContent(NotificationRequest request) {
        StringBuilder content = new StringBuilder();
        content.append("Notification from ").append(appName).append("\n\n");
        if (request.getType() != null) {
            content.append("Type: ").append(request.getType()).append("\n");
        }
        if (request.getContent() != null) {
            content.append("Message: ").append(request.getContent());
        } else {
            content.append("No message content provided.");
        }
        return content.toString();
    }
}