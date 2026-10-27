package com.urbano.monolith.audit.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.audit.dto.AuditLogDto;
import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.entity.AuditLog;
import com.urbano.monolith.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Asynchronously log an action - non-blocking.
     * Automatically extracts userId and pmAccountId from TenantContext if not explicitly provided.
     */
    @Async
    @Transactional
    public void logAction(AuditLogRequest request) {
        try {
            UUID userId = request.getUserId() != null ? request.getUserId() : TenantContext.getUserId();
            UUID pmAccountId = request.getPmAccountId() != null ? request.getPmAccountId() : TenantContext.getPmAccountId();

            AuditLog auditLog = AuditLog.builder()
                    .eventType(request.getEventType())
                    .userId(userId != null ? userId.toString() : null)
                    .pmAccountId(pmAccountId != null ? pmAccountId.toString() : null)
                    .username(request.getUsername())
                    .action(request.getAction())
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId() != null ? request.getResourceId().toString() : null)
                    .metadata(request.getMetadata())
                    .details(request.getMetadata())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .isSuccess(request.isSuccess())
                    .executionTime(request.getExecutionTime())
                    .correlationId(request.getCorrelationId())
                    .serviceName(request.getServiceName() != null ? request.getServiceName() : "monolith")
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {} (pmAccountId={})", request.getEventType(), request.getAction(), pmAccountId);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Get a single audit log by ID
     */
    @Transactional(readOnly = true)
    public AuditLogDto getAuditLog(UUID id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));

        // Ownership check: non-superadmin can only view logs belonging to their pmAccountId
        if (!TenantContext.isAdminBypass()) {
            UUID pmAccountId = TenantContext.getPmAccountId();
            if (pmAccountId == null || auditLog.getPmAccountId() == null
                    || !pmAccountId.toString().equals(auditLog.getPmAccountId())) {
                throw new UnauthorizedException("Access denied to this audit log");
            }
        }

        return mapToDto(auditLog);
    }

    /**
     * Get audit logs for a specific user with pagination
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getUserAuditLogs(UUID userId, int page, int size) {
        if (!TenantContext.isAdminBypass() && !userId.equals(TenantContext.getUserId())) {
            throw new UnauthorizedException("Access denied to audit logs for this user");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditPage = auditLogRepository
                .findByUserIdOrderByTimestampDesc(userId.toString(), pageable);
        return mapToPagedResponse(auditPage);
    }

    /**
     * Get audit logs by event type with pagination
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByEventType(String eventType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditPage = auditLogRepository.findByEventType(eventType, pageable);
        return mapToPagedResponse(auditPage);
    }

    /**
     * Get audit logs by action with pagination
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByAction(String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditPage = auditLogRepository.findByAction(action, pageable);
        return mapToPagedResponse(auditPage);
    }

    /**
     * Get recent audit logs (last 24 hours).
     * If called by PM_ADMIN or PM_STAFF, strictly scoped to their pmAccountId.
     * If called by SUPER_ADMIN, shows platform-wide recent logs.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getRecentAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditPage;

        if (TenantContext.isAdminBypass()) {
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            auditPage = auditLogRepository.findByTimestampAfter(twentyFourHoursAgo, pageable);
        } else {
            UUID pmAccountId = TenantContext.getPmAccountId();
            if (pmAccountId == null) {
                throw new UnauthorizedException("No tenant context — authentication required");
            }
            auditPage = auditLogRepository.findByPmAccountIdOrderByTimestampDesc(pmAccountId.toString(), pageable);
        }

        return mapToPagedResponse(auditPage);
    }

    /**
     * Get audit logs by service name
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByService(String serviceName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditPage = auditLogRepository.findByServiceName(serviceName, pageable);
        return mapToPagedResponse(auditPage);
    }

    /**
     * Get failed audit logs
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getFailedAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditPage = auditLogRepository.findByIsSuccessFalse(pageable);
        return mapToPagedResponse(auditPage);
    }

    /**
     * Map AuditLog entity to DTO
     */
    private AuditLogDto mapToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .eventType(auditLog.getEventType())
                .userId(auditLog.getUserId() != null ? UUID.fromString(auditLog.getUserId()) : null)
                .pmAccountId(auditLog.getPmAccountId() != null ? UUID.fromString(auditLog.getPmAccountId()) : null)
                .username(auditLog.getUsername())
                .action(auditLog.getAction())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId() != null ? UUID.fromString(auditLog.getResourceId()) : null)
                .metadata(auditLog.getMetadata())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .timestamp(auditLog.getTimestamp())
                .success(auditLog.getIsSuccess() != null && auditLog.getIsSuccess())
                .executionTime(auditLog.getExecutionTime())
                .correlationId(auditLog.getCorrelationId())
                .serviceName(auditLog.getServiceName())
                .build();
    }

    /**
     * Map Page to PagedResponse
     */
    private PagedResponse<AuditLogDto> mapToPagedResponse(Page<AuditLog> page) {
        List<AuditLogDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<AuditLogDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
