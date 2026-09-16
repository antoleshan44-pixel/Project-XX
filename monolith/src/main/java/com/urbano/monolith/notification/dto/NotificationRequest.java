package com.urbano.monolith.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "PM Account ID is required")
    private UUID pmAccountId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Channel is required")
    private String channel; // EMAIL, SMS, PUSH, IN_APP

    @NotBlank(message = "Type is required")
    private String type; // RENT_REMINDER, LEASE_CONFIRMATION, MAINTENANCE_UPDATE, etc.

    @NotBlank(message = "Recipient is required")
    @Email(message = "Invalid email format for email channel")
    private String recipient;

    private String subject;
    private String content;

    // ✅ Template support
    private String templateName;
    private Map<String, Object> templateData;

    // ✅ Priority
    @Builder.Default
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
}