package com.urbano.monolith.admin.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PmAccountStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.admin.dto.AdminPmAccountDto;
import com.urbano.monolith.audit.constants.AuditEventType;
import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.service.AuditService;
import com.urbano.monolith.auth.entity.PmAccount;
import com.urbano.monolith.auth.repository.PmAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPmAccountService {

    private final PmAccountRepository pmAccountRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<AdminPmAccountDto> list(PmAccountStatus status, Pageable pageable) {
        Page<PmAccount> page = (status == null)
                ? pmAccountRepository.findAll(pageable)
                : pmAccountRepository.findByApprovalStatus(status, pageable);
        return toPaged(page);
    }

    @Transactional
    public AdminPmAccountDto suspend(UUID pmAccountId, String reason) {
        PmAccount pm = pmAccountRepository.findById(pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("PM account not found"));

        if (pm.getApprovalStatus() == PmAccountStatus.SUSPENDED) {
            return toDto(pm); // already suspended — idempotent
        }

        pm.setApprovalStatus(PmAccountStatus.SUSPENDED);
        pm.setIsActive(false);
        pm.setSuspendedReason(reason);
        pm.setSuspendedAt(LocalDateTime.now());
        pm.setSuspendedBy(TenantContext.getUserId());
        pmAccountRepository.save(pm);

        audit("PM_ACCOUNT_SUSPENDED", pmAccountId, reason);
        log.info("[admin] PM account {} SUSPENDED by {} — reason: {}",
                pmAccountId, TenantContext.getUserId(), reason);
        return toDto(pm);
    }

    @Transactional
    public AdminPmAccountDto reactivate(UUID pmAccountId, String reason) {
        PmAccount pm = pmAccountRepository.findById(pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("PM account not found"));

        if (pm.getApprovalStatus() == PmAccountStatus.ACTIVE) {
            return toDto(pm);
        }

        pm.setApprovalStatus(PmAccountStatus.ACTIVE);
        pm.setIsActive(true);
        pm.setSuspendedReason(null);
        pm.setSuspendedAt(null);
        pm.setSuspendedBy(null);
        pmAccountRepository.save(pm);

        audit("PM_ACCOUNT_REACTIVATED", pmAccountId, reason);
        log.info("[admin] PM account {} REACTIVATED by {} — reason: {}",
                pmAccountId, TenantContext.getUserId(), reason);
        return toDto(pm);
    }

    // -----------------------------------------------------------------

    private void audit(String action, UUID pmAccountId, String reason) {
        AuditLogRequest req = AuditLogRequest.builder()
                .eventType(AuditEventType.USER_UPDATED)
                .userId(TenantContext.getUserId())
                .username("SUPER_ADMIN")
                .action(action)
                .resourceType("PM_ACCOUNT")
                .resourceId(pmAccountId)
                .metadata(java.util.Map.of("reason", reason == null ? "" : reason))
                .success(true)
                .serviceName("admin")
                .build();
        auditService.logAction(req);
    }

    private AdminPmAccountDto toDto(PmAccount pm) {
        return AdminPmAccountDto.builder()
                .id(pm.getId())
                .companyName(pm.getCompanyName())
                .serviceOption(pm.getServiceOption())
                .isActive(pm.getIsActive())
                .approvalStatus(pm.getApprovalStatus())
                .suspendedReason(pm.getSuspendedReason())
                .suspendedAt(pm.getSuspendedAt())
                .suspendedBy(pm.getSuspendedBy())
                .createdAt(pm.getCreatedAt())
                .updatedAt(pm.getUpdatedAt())
                .build();
    }

    private PagedResponse<AdminPmAccountDto> toPaged(Page<PmAccount> page) {
        List<AdminPmAccountDto> content = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return PagedResponse.<AdminPmAccountDto>builder()
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