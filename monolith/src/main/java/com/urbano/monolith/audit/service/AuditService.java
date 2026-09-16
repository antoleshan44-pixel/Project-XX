package com.urbano.monolith.audit.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
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
     * Asynchronously log an action - non-blocking
     */
    @Async
    @Transactional
    public void logAction(AuditLogRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventType(request.getEventType())
                    .userId(request.getUserId() != null ? request.getUserId().toString() : null)
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
                    .serviceName(request.getServiceName())
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {}", request.getEventType(), request.getAction());
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
        return mapToDto(auditLog);
    }

    /**
     * Get audit logs for a specific user with pagination
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getUserAuditLogs(UUID userId, int page, int size) {
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
     * Get recent audit logs (last 24 hours)
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getRecentAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        Page<AuditLog> auditPage = auditLogRepository.findByTimestampAfter(twentyFourHoursAgo, pageable);
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