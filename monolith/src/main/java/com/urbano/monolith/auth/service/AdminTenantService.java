package com.urbano.monolith.auth.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.InviteStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.auth.dto.AdminInviteCodeDto;
import com.urbano.monolith.auth.dto.AdminTenantDto;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SUPER_ADMIN-only tenant inspection + support endpoints.
 *
 * <p>Used by the platform support team to help a tenant who did not receive
 * their invite SMS (a real production scenario once Twilio is enabled).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final InviteTokenService inviteTokenService;

    // ============================================================
    // LIST (all PM accounts, optionally filtered by invite status)
    // ============================================================

    @Transactional(readOnly = true)
    public PagedResponse<AdminTenantDto> list(InviteStatus status, Pageable pageable) {
        Page<Tenant> page = (status == null)
                ? tenantRepository.findAll(pageable)
                : tenantRepository.findByInviteStatus(status, pageable);
        return toPaged(page);
    }

    // ============================================================
    // INVITE CODE LOOKUP (support tooling)
    // ============================================================

    /**
     * Returns the current invite code for a PENDING tenant without consuming
     * it. Used by the platform super-admin to help a tenant whose SMS never
     * arrived (Twilio misconfiguration, carrier filter, etc.).
     *
     * <p>Fails with:</p>
     * <ul>
     *   <li>{@link ResourceNotFoundException} — tenant does not exist</li>
     *   <li>{@link ConflictException} — tenant is not PENDING (already activated or MANUAL)</li>
     * </ul>
     *
     * <p>Returns {@code code = null} if the tenant is PENDING but no code
     * exists in Redis (expired or never sent). The caller can then call
     * {@code POST /api/tenants/{id}/resend-invite} as PM, or we can add a
     * super-admin resend endpoint later.</p>
     */
    @Transactional(readOnly = true)
    public AdminInviteCodeDto getInviteCode(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (tenant.getInviteStatus() != InviteStatus.PENDING) {
            throw new ConflictException(
                    "Tenant is not PENDING (current status: " + tenant.getInviteStatus() + ")");
        }

        String code = inviteTokenService.peek(tenantId.toString());
        Long ttl = inviteTokenService.ttlSeconds(tenantId.toString());

        log.info("[admin] Invite code lookup for tenant {} — code present: {}, ttl: {}s",
                tenantId, code != null, ttl);

        return AdminInviteCodeDto.builder()
                .tenantId(tenant.getId())
                .tenantFullName(tenant.getFullName())
                .tenantPhone(tenant.getPhone())
                .tenantEmail(tenant.getEmail())
                .inviteStatus(tenant.getInviteStatus().name())
                .code(code)
                .ttlSeconds(ttl)
                .build();
    }

    // ============================================================
    // MAPPERS
    // ============================================================

    private AdminTenantDto toDto(Tenant t) {
        return AdminTenantDto.builder()
                .id(t.getId())
                .pmAccountId(t.getPmAccountId())
                .userId(t.getUserId())
                .fullName(t.getFullName())
                .email(t.getEmail())
                .phone(t.getPhone())
                .unitId(t.getUnitId())
                .isActive(t.getIsActive())
                .creditBalance(t.getCreditBalance())
                .inviteStatus(t.getInviteStatus().name())
                .invitedAt(t.getInvitedAt())
                .activatedAt(t.getActivatedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private PagedResponse<AdminTenantDto> toPaged(Page<Tenant> page) {
        List<AdminTenantDto> content = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return PagedResponse.<AdminTenantDto>builder()
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