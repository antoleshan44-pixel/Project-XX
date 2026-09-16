package com.urbano.monolith.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private UUID pmAccountId;
    private UUID userId;
    private String type;
    private String channel;
    private String recipient;
    private String subject;
    private String content;
    private String status;
    private boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private Integer deliveryAttempts;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}