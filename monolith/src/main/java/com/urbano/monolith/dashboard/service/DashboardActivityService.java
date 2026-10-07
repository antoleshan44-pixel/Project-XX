package com.urbano.monolith.dashboard.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.audit.entity.AuditLog;
import com.urbano.monolith.audit.repository.AuditLogRepository;
import com.urbano.monolith.dashboard.dto.ActivityItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped activity feed for the PM dashboard.
 * Reads from audit_logs filtered by pm_account_id (added in V9).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardActivityService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 100;

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<ActivityItemDto> getRecentActivity(int limit) {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("PM account context required");
        }

        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);

        var page = auditLogRepository.findByPmAccountIdOrderByTimestampDesc(
                pmAccountId.toString(),
                PageRequest.of(0, capped, Sort.by(Sort.Direction.DESC, "timestamp")));

        return page.getContent().stream().map(this::toDto).toList();
    }

    private ActivityItemDto toDto(AuditLog log) {
        return ActivityItemDto.builder()
                .id(log.getId())
                .userId(parseUuidSafe(log.getUserId()))
                .username(log.getUsername())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .eventType(log.getEventType())
                .success(log.getIsSuccess())
                .timestamp(log.getTimestamp())
                .build();
    }

    private static UUID parseUuidSafe(String s) {
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}