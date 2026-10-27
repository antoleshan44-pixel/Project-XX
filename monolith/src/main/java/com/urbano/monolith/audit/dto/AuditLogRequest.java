package com.urbano.monolith.audit.dto;

import jakarta.validation.constraints.NotBlank;
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
public class AuditLogRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    private UUID userId;

    private UUID pmAccountId;

    private String username;

    @NotBlank(message = "Action is required")
    private String action;

    private String resourceType;

    private UUID resourceId;

    private Map<String, Object> metadata;

    private String ipAddress;

    private String userAgent;

    @Builder.Default
    private boolean success = true;

    private Long executionTime;

    private String correlationId;

    private String serviceName;
}
