package com.urbano.monolith.audit.event;

import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralized audit event publisher for all services to use
 * via Feign client or direct call
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final AuditService auditService;

    public void publishAuditEvent(String eventType, UUID userId, String username,
                                  String action, String resourceType, UUID resourceId,
                                  Object metadata, boolean success) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .eventType(eventType)
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .metadata(convertToMap(metadata))
                    .success(success)
                    .serviceName("urbano-homes")
                    .build();
            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to publish audit event: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> convertToMap(Object metadata) {
        if (metadata == null) return null;
        if (metadata instanceof java.util.Map) {
            return (java.util.Map<String, Object>) metadata;
        }
        // Simple conversion for common types
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("data", metadata);
        return map;
    }
}