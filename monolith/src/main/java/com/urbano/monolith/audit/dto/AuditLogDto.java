package com.urbano.monolith.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private UUID id;
    private String eventType;
    private UUID userId;
    private String username;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private Map<String, Object> metadata;
    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private boolean success;
    private Long executionTime;
    private String correlationId;
    private String serviceName;
}