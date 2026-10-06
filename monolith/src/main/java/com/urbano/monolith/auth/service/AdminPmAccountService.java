package com.urbano.monolith.auth.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PmAccountStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.service.AuditService;
import com.urbano.monolith.auth.dto.AdminPmAccountDto;
import com.urbano.monolith.auth.entity.PmAccount;
import com.urbano.monolith.auth.repository.PmAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
                .orElseThrow(() -> new ResourceNotFoundException("PM Account not found"));

        if (pm.getApprovalStatus() == PmAccountStatus.SUSPENDED) {
            return toDto(pm);
        }

        pm.setApprovalStatus(PmAccountStatus.SUSPENDED);
        pm.setSuspendedReason(reason);
        pmAccountRepository.save(pm);

        audit("PM_ACCOUNT_SUSPENDED", pmAccountId, reason);
        log.info("[admin] PM Account {} SUSPENDED by {}", pmAccountId, TenantContext.getUserId());
        return toDto(pm);
    }

    @Transactional
    public AdminPmAccountDto reactivate(UUID pmAccountId, String reason) {
        PmAccount pm = pmAccountRepository.findById(pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("PM Account not found"));

        if (pm.getApprovalStatus() != PmAccountStatus.SUSPENDED) {
            throw new ConflictException("Only SUSPENDED accounts can be reactivated");
        }

        pm.setApprovalStatus(PmAccountStatus.ACTIVE);
        pm.setSuspendedReason(null);
        pmAccountRepository.save(pm);

        audit("PM_ACCOUNT_REACTIVATED", pmAccountId, reason);
        log.info("[admin] PM Account {} REACTIVATED by {}", pmAccountId, TenantContext.getUserId());
        return toDto(pm);
    }

    private void audit(String action, UUID pmAccountId, String reason) {
        AuditLogRequest req = AuditLogRequest.builder()
                .eventType("PM_ACCOUNT_STATUS_CHANGED")
                .userId(TenantContext.getUserId())
                .username("SUPER_ADMIN")
                .action(action)
                .resourceType("PM_ACCOUNT")
                .resourceId(pmAccountId)
                .metadata(Map.of("reason", reason == null ? "" : reason))
                .success(true)
                .serviceName("admin")
                .build();
        auditService.logAction(req);
    }

    private AdminPmAccountDto toDto(PmAccount pm) {
        return AdminPmAccountDto.builder()
                .id(pm.getId())
                .name(pm.getCompanyName())
                .companyRegNumber(null)
                .status(pm.getApprovalStatus())
                .approvedAt(null)
                .approvedBy(null)
                .rejectionReason(null)
                .suspendedReason(pm.getSuspendedReason())
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
