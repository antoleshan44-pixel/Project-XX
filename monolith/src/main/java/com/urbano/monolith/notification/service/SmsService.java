package com.urbano.monolith.notification.service;

import com.urbano.common.exception.SmsDeliveryException;
import com.urbano.monolith.auth.config.AfricaTalkingConfig;
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
    private final AfricaTalkingConfig africaTalkingConfig;

    @Value("${sms.provider:twilio}")
    private String smsProvider;

    // Twilio
    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        if ("twilio".equalsIgnoreCase(smsProvider)) {
            if (isNonBlank(twilioAccountSid) && isNonBlank(twilioAuthToken)) {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                log.info("Twilio SMS provider initialized");
            } else {
                log.warn("sms.provider=twilio but Twilio credentials are incomplete. " +
                        "SMS sends will fail until twilio.account.sid and twilio.auth.token are set.");
            }
        } else if ("africastalking".equalsIgnoreCase(smsProvider)) {
            if (africaTalkingConfig.isConfigured()) {
                log.info("Africa's Talking SMS provider initialized (username={})",
                        africaTalkingConfig.getUsername());
            } else {
                log.warn("sms.provider=africastalking but africastalking.username/apikey " +
                        "are missing. SMS sends will fail.");
            }
        } else {
            log.warn("Unknown sms.provider='{}'. Supported: twilio, africastalking.", smsProvider);
        }
    }

    /**
     * Send SMS via the configured provider.
     * Throws SmsDeliveryException on any delivery failure (no silent success).
     */
    public void sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new SmsDeliveryException("Recipient phone number is blank");
        }

        String formattedPhone = formatPhoneNumber(phoneNumber);

        try {
            if ("twilio".equalsIgnoreCase(smsProvider)) {
                if (!isNonBlank(twilioAccountSid) || !isNonBlank(twilioAuthToken)
                        || !isNonBlank(twilioPhoneNumber)) {
                    throw new SmsDeliveryException("Twilio is not fully configured");
                }
                sendViaTwilio(formattedPhone, message);
            } else if ("africastalking".equalsIgnoreCase(smsProvider)) {
                if (!africaTalkingConfig.isConfigured()) {
                    throw new SmsDeliveryException("Africa's Talking is not configured");
                }
                sendViaAfricaTalking(formattedPhone, message);
            } else {
                throw new SmsDeliveryException("Unknown SMS provider: " + smsProvider);
            }
            log.info("SMS sent to {}", maskPhone(phoneNumber));
        } catch (SmsDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhone(phoneNumber), e.getMessage());
            throw new SmsDeliveryException("SMS sending failed: " + e.getMessage(), e);
        }
    }

    private void sendViaTwilio(String phoneNumber, String message) {
        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                message
        ).create();
    }

    private void sendViaAfricaTalking(String phoneNumber, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", africaTalkingConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", africaTalkingConfig.getUsername());
        requestBody.put("to", phoneNumber);
        requestBody.put("message", message);
        requestBody.put("sender", africaTalkingConfig.getSenderId());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = africaTalkingConfig.getBaseUrl() + "/messaging";
        restTemplate.postForObject(url, entity, String.class);
    }

    public void sendNotificationSms(NotificationRequest request) {
        sendSms(request.getRecipient(), generateSmsContent(request));
    }

    private String generateSmsContent(NotificationRequest request) {
        StringBuilder content = new StringBuilder("Urbano Homes: ");
        switch (request.getType()) {
            case "RENT_REMINDER" -> content.append("Rent reminder - ").append(request.getContent());
            case "LEASE_CONFIRMATION" -> content.append("Lease confirmed - ").append(request.getContent());
            case "MAINTENANCE_UPDATE" -> content.append("Maintenance update - ").append(request.getContent());
            case "PAYMENT_RECEIVED" -> content.append("Payment received - ").append(request.getContent());
            case "TENANT_INVITE" -> content.append("Welcome to Urbano Homes! ").append(request.getContent());
            default -> content.append(request.getContent());
        }
        if (content.length() > 160) {
            return content.substring(0, 157) + "...";
        }
        return content.toString();
    }

    private String formatPhoneNumber(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            return "254" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("254")) {
            return "254" + cleaned;
        }
        return cleaned;
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}