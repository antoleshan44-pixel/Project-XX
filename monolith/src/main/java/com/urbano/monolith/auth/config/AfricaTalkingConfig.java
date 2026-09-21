package com.urbano.monolith.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "africatalking")
public class AfricaTalkingConfig {
    private String username;
    private String apiKey;
    private String senderId = "UrbanoHomes";
    private String baseUrl = "https://api.africastalking.com/version1";

    @PostConstruct
    void validate() {
        // Warn only — Twilio may be the active provider; we still want the app to boot.
        // The send-path (SmsService.sendSms) will throw SmsDeliveryException if AT is
        // selected but not configured.
        if (username == null || username.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Africa's Talking config incomplete (username/apikey missing). " +
                    "SMS via Africa's Talking will fail if sms.provider=africastalking.");
        }
    }

    public boolean isConfigured() {
        return username != null && !username.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }
}