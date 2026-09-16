package com.urbano.monolith.notification.service;

import com.urbano.monolith.notification.dto.NotificationRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${sms.provider:twilio}")
    private String smsProvider;

    // Twilio Configuration
    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;

    // Africa's Talking Configuration
    @Value("${africastalking.username:}")
    private String atUsername;

    @Value("${africastalking.api-key:}")
    private String atApiKey;

    @Value("${africastalking.sender-id:URBANO}")
    private String atSenderId;

    @PostConstruct
    public void init() {
        if ("twilio".equalsIgnoreCase(smsProvider) && twilioAccountSid != null && !twilioAccountSid.isEmpty()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("Twilio SMS service initialized");
        }
    }

    /**
     * Send SMS using configured provider
     */
    public void sendSms(String phoneNumber, String message) {
        String formattedPhone = formatPhoneNumber(phoneNumber);

        try {
            if ("twilio".equalsIgnoreCase(smsProvider)) {
                sendViaTwilio(formattedPhone, message);
            } else if ("africastalking".equalsIgnoreCase(smsProvider)) {
                sendViaAfricaTalking(formattedPhone, message);
            } else {
                log.warn("No SMS provider configured, logging SMS instead");
                log.info("SMS to {}: {}", formattedPhone, message);
            }
            log.info("SMS sent to: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("SMS sending failed", e);
        }
    }

    /**
     * Send SMS via Twilio
     */
    private void sendViaTwilio(String phoneNumber, String message) {
        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                message
        ).create();
    }

    /**
     * Send SMS via Africa's Talking
     */
    private void sendViaAfricaTalking(String phoneNumber, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", atApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", atUsername);
        requestBody.put("to", phoneNumber);
        requestBody.put("message", message);
        requestBody.put("sender", atSenderId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = "https://api.africastalking.com/version1/messaging";
        restTemplate.postForObject(url, entity, String.class);
    }

    /**
     * Send notification SMS
     */
    public void sendNotificationSms(NotificationRequest request) {
        String phone = request.getRecipient();
        String message = generateSmsContent(request);
        sendSms(phone, message);
    }

    private String generateSmsContent(NotificationRequest request) {
        StringBuilder content = new StringBuilder();
        content.append("Urbano Homes: ");

        switch (request.getType()) {
            case "RENT_REMINDER" -> content.append("Rent reminder - ").append(request.getContent());
            case "LEASE_CONFIRMATION" -> content.append("Lease confirmed - ").append(request.getContent());
            case "MAINTENANCE_UPDATE" -> content.append("Maintenance update - ").append(request.getContent());
            case "PAYMENT_RECEIVED" -> content.append("Payment received - ").append(request.getContent());
            case "TENANT_INVITE" -> content.append("Welcome to Urbano Homes! ").append(request.getContent());
            default -> content.append(request.getContent());
        }

        // Truncate if too long for SMS (160 chars)
        if (content.length() > 160) {
            return content.substring(0, 157) + "...";
        }
        return content.toString();
    }

    private String formatPhoneNumber(String phone) {
        // Remove any non-digit characters
        String cleaned = phone.replaceAll("[^0-9]", "");
        // If starts with 0, replace with 254 (Kenya)
        if (cleaned.startsWith("0")) {
            return "254" + cleaned.substring(1);
        }
        // If doesn't start with 254, add it
        if (!cleaned.startsWith("254")) {
            return "254" + cleaned;
        }
        return cleaned;
    }
}