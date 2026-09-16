package com.urbano.monolith.audit.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.audit.dto.AuditLogDto;
import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Log an action asynchronously
     */
    @PostMapping("/log")
    public ResponseEntity<Void> logAction(@Valid @RequestBody AuditLogRequest request) {
        auditService.logAction(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Get a single audit log by ID
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<AuditLogDto> getAuditLog(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(auditService.getAuditLog(id));
    }

    /**
     * Get audit logs for a specific user
     */
    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<PagedResponse<AuditLogDto>> getUserAuditLogs(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getUserAuditLogs(userId, page, size));
    }

    /**
     * Get audit logs by event type
     */
    @GetMapping("/logs/event/{eventType}")
    public ResponseEntity<PagedResponse<AuditLogDto>> getAuditLogsByEventType(
            @PathVariable("eventType") String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByEventType(eventType, page, size));
    }

    /**
     * Get audit logs by action
     */
    @GetMapping("/logs/action/{action}")
    public ResponseEntity<PagedResponse<AuditLogDto>> getAuditLogsByAction(
            @PathVariable("action") String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByAction(action, page, size));
    }

    /**
     * Get recent audit logs (last 24 hours)
     */
    @GetMapping("/logs/recent")
    public ResponseEntity<PagedResponse<AuditLogDto>> getRecentAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getRecentAuditLogs(page, size));
    }

    /**
     * Get audit logs by service
     */
    @GetMapping("/logs/service/{serviceName}")
    public ResponseEntity<PagedResponse<AuditLogDto>> getAuditLogsByService(
            @PathVariable("serviceName") String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByService(serviceName, page, size));
    }

    /**
     * Get failed audit logs
     */
    @GetMapping("/logs/failed")
    public ResponseEntity<PagedResponse<AuditLogDto>> getFailedAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getFailedAuditLogs(page, size));
    }
}