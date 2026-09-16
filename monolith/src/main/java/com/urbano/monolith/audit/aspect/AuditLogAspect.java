package com.urbano.monolith.audit.aspect;

import com.urbano.monolith.audit.constants.AuditEventType;
import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.service.AuditService;
import com.urbano.common.context.CorrelationIdContext;
import com.urbano.common.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;  // ✅ ADD THIS IMPORT

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditService auditService;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void writeOperations() {}

    @Around("writeOperations()")
    public Object auditWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String action = joinPoint.getSignature().getName();
        String eventType = determineEventType(joinPoint);

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logAudit(eventType, action, success, executionTime, joinPoint);
        }
    }

    private void logAudit(String eventType, String action, boolean success, long executionTime, ProceedingJoinPoint joinPoint) {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                    .eventType(eventType)
                    .userId(extractUserId(request))
                    .username(extractUsername(request))
                    .action(action)
                    .resourceType(extractResourceType(joinPoint))
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .success(success)
                    .executionTime(executionTime)
                    .correlationId(CorrelationIdContext.getCorrelationId())
                    .serviceName("audit-service")
                    .build();

            auditService.logAction(auditRequest);
        } catch (Exception e) {
            log.warn("Failed to log audit: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private UUID extractUserId(HttpServletRequest request) {
        if (request == null) return null;
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private String extractUsername(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("X-User-Email");
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }

    private String determineEventType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (className.contains("Auth")) {
            if (methodName.contains("register")) return AuditEventType.AUTH_REGISTER;
            if (methodName.contains("login")) return AuditEventType.AUTH_LOGIN;
            if (methodName.contains("logout")) return AuditEventType.AUTH_LOGOUT;
            if (methodName.contains("refresh")) return AuditEventType.AUTH_REFRESH;
            if (methodName.contains("reset")) return AuditEventType.AUTH_PASSWORD_RESET;
            if (methodName.contains("verify") || methodName.contains("confirm"))
                return AuditEventType.AUTH_PHONE_VERIFY;
        }

        if (className.contains("Property")) {
            if (methodName.contains("create")) return AuditEventType.PROPERTY_CREATED;
            if (methodName.contains("update")) return AuditEventType.PROPERTY_UPDATED;
            if (methodName.contains("delete")) return AuditEventType.PROPERTY_DELETED;
            if (methodName.contains("status")) return AuditEventType.PROPERTY_STATUS_CHANGED;
        }

        if (className.contains("Unit")) {
            if (methodName.contains("create")) return AuditEventType.UNIT_CREATED;
            if (methodName.contains("update")) return AuditEventType.UNIT_UPDATED;
            if (methodName.contains("delete")) return AuditEventType.UNIT_DELETED;
            if (methodName.contains("publish")) return AuditEventType.UNIT_PUBLISHED;
            if (methodName.contains("unpublish")) return AuditEventType.UNIT_UNPUBLISHED;
            if (methodName.contains("status")) return AuditEventType.UNIT_STATUS_CHANGED;
        }

        if (className.contains("Tenant")) {
            if (methodName.contains("create")) return AuditEventType.TENANT_CREATED;
            if (methodName.contains("update")) return AuditEventType.TENANT_UPDATED;
            if (methodName.contains("delete")) return AuditEventType.TENANT_DELETED;
            if (methodName.contains("invite")) return AuditEventType.TENANT_INVITED;
        }

        if (className.contains("Lease")) {
            if (methodName.contains("create")) return AuditEventType.LEASE_CREATED;
            if (methodName.contains("terminate")) return AuditEventType.LEASE_TERMINATED;
            if (methodName.contains("update")) return AuditEventType.LEASE_UPDATED;
        }

        if (className.contains("Payment") || className.contains("Reconciliation")) {
            if (methodName.contains("create")) return AuditEventType.PAYMENT_CREATED;
            if (methodName.contains("reconcile")) return AuditEventType.PAYMENT_RECONCILED;
            if (methodName.contains("unreconcile")) return AuditEventType.PAYMENT_UNRECONCILED;
            if (methodName.contains("failed")) return AuditEventType.PAYMENT_FAILED;
        }

        if (className.contains("Maintenance")) {
            if (methodName.contains("create") || methodName.contains("submit"))
                return AuditEventType.MAINTENANCE_REQUESTED;
            if (methodName.contains("update")) return AuditEventType.MAINTENANCE_UPDATED;
            if (methodName.contains("resolve") || methodName.contains("complete"))
                return AuditEventType.MAINTENANCE_RESOLVED;
        }

        return "UNKNOWN_" + className + "_" + methodName;
    }

    private String extractResourceType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        if (className.contains("Auth")) return "AUTH";
        if (className.contains("Property")) return "PROPERTY";
        if (className.contains("Unit")) return "UNIT";
        if (className.contains("Tenant")) return "TENANT";
        if (className.contains("Lease")) return "LEASE";
        if (className.contains("Payment")) return "PAYMENT";
        if (className.contains("Maintenance")) return "MAINTENANCE";
        if (className.contains("Viewing")) return "VIEWING";
        if (className.contains("User")) return "USER";
        return "UNKNOWN";
    }
}