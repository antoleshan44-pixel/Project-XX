package com.urbano.monolith.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "africatalking")
public class AfricaTalkingConfig {
    private String username;
    private String apiKey;
    private String senderId = "UrbanoHomes";
    private String baseUrl = "https://api.africastalking.com/version1";
}