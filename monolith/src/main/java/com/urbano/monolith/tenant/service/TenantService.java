package com.urbano.monolith.tenant.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.InviteStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.auth.service.InviteTokenService;
import com.urbano.monolith.notification.service.SmsService;
import com.urbano.monolith.tenant.dto.TenantDto;
import com.urbano.monolith.tenant.dto.TenantInviteRequest;
import com.urbano.monolith.tenant.dto.TenantInviteResponse;
import com.urbano.monolith.tenant.dto.TenantRequest;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final InviteTokenService inviteTokenService;
    private final SmsService smsService;

    @Value("${app.mobile.deep-link-prefix:urbanohomes://tenant/activate}")
    private String deepLinkPrefix;

    // ============================================================
    // COMMIT 6b: INVITE FLOW
    // ============================================================

    /**
     * Creates a PENDING tenant row and sends an invite SMS with both a deep
     * link and a 6-digit fallback code.
     *
     * <p>Idempotency: if a tenant already exists for this (pmAccountId, phone),
     * we return 409. Use resend-invite to reuse an existing pending row.</p>
     */
    @Transactional
    public TenantInviteResponse inviteTenant(TenantInviteRequest request, UUID pmAccountId) {
        if (pmAccountId == null) {
            throw new com.urbano.common.exception.UnauthorizedException("No tenant context");
        }

        // Duplicate check: any tenant (active, pending, or manual) with this phone in this PM account
        tenantRepository.findByPhone(request.getPhone())
                .ifPresent(existing -> {
                    if (pmAccountId.equals(existing.getPmAccountId())) {
                        throw new ConflictException(
                                "A tenant already exists for phone " + request.getPhone()
                                        + " (status=" + existing.getInviteStatus()
                                        + "). Use resend-invite if the invite is still PENDING.");
                    } else {
                        throw new ConflictException("Phone number is already registered to another PM account");
                    }
                });

        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }

        String[] nameParts = request.getFullName() != null
                ? request.getFullName().trim().split("\\s+", 2)
                : new String[]{"Unknown", ""};
        String firstName = nameParts[0].isEmpty() ? "Unknown" : nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Tenant tenant = Tenant.builder()
                .pmAccountId(pmAccountId)
                .userId(null)                       // set on activate
                .fullName(request.getFullName())
                .firstName(firstName)
                .lastName(lastName)
                .email(request.getEmail())
                .phone(request.getPhone())
                .unitId(request.getUnitId())
                .isActive(false)                    // activated later
                .creditBalance(0.0)
                .inviteStatus(InviteStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant invited: {} for PM account {}", tenant.getId(), pmAccountId);

        boolean smsDispatched = sendInviteSms(tenant);

        return TenantInviteResponse.builder()
                .tenantId(tenant.getId())
                .pmAccountId(pmAccountId)
                .fullName(tenant.getFullName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .unitId(tenant.getUnitId())
                .inviteStatus(tenant.getInviteStatus())
                .invitedAt(tenant.getInvitedAt())
                .smsDispatched(smsDispatched)
                .message(smsDispatched
                        ? "Invite sent. Recipient has 10 minutes to enter the code."
                        : "Tenant created but SMS delivery failed. Use resend-invite to retry.")
                .build();
    }

    /**
     * Regenerates the 6-digit code and resends the SMS for an existing PENDING tenant.
     * No-op-safe if the tenant was already activated.
     */
    @Transactional
    public TenantInviteResponse resendInvite(UUID tenantId, UUID pmAccountId) {
        Tenant tenant = tenantRepository.findByIdAndPmAccountId(tenantId, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (tenant.getInviteStatus() == InviteStatus.ACTIVATED) {
            throw new ConflictException("Tenant has already activated their account");
        }

        // Move to PENDING if it was somehow MANUAL (shouldn't happen, but defensive)
        tenant.setInviteStatus(InviteStatus.PENDING);
        tenant.setInvitedAt(LocalDateTime.now());
        tenant = tenantRepository.save(tenant);

        boolean smsDispatched = sendInviteSms(tenant);

        return TenantInviteResponse.builder()
                .tenantId(tenant.getId())
                .pmAccountId(tenant.getPmAccountId())
                .fullName(tenant.getFullName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .unitId(tenant.getUnitId())
                .inviteStatus(tenant.getInviteStatus())
                .invitedAt(tenant.getInvitedAt())
                .smsDispatched(smsDispatched)
                .message(smsDispatched
                        ? "Invite re-sent. Recipient has 10 minutes to enter the code."
                        : "SMS delivery failed. Please try again.")
                .build();
    }

    private boolean sendInviteSms(Tenant tenant) {
        String code = inviteTokenService.generateCodeForTenant(tenant.getId().toString());
        String deepLink = deepLinkPrefix + "?phone=" + tenant.getPhone();  // no code in the link

        String message = String.format(
                "You're invited to Urbano Homes. Open the app: %s — or enter code %s. Expires in 10 minutes.",
                deepLink, code);

        try {
            smsService.sendSms(tenant.getPhone(), message);
            return true;
        } catch (Exception e) {
            // Don't roll back the tenant row — the PM can retry via resend-invite.
            log.error("Failed to dispatch invite SMS for tenant {}: {}", tenant.getId(), e.getMessage());
            return false;
        }
    }

    // ============================================================
    // EXISTING METHODS (unchanged behavior + new fields in mapToDto)
    // ============================================================

    @Transactional
    public TenantDto createTenant(TenantRequest request) {
        UUID pmAccountId = com.urbano.common.context.TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new com.urbano.common.exception.UnauthorizedException("No tenant context");
        }

        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        if (tenantRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already exists: " + request.getPhone());
        }

        String[] nameParts = request.getFullName() != null
                ? request.getFullName().trim().split("\\s+", 2)
                : new String[]{"Unknown", ""};
        String firstName = nameParts[0].isEmpty() ? "Unknown" : nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Tenant tenant = Tenant.builder()
                .pmAccountId(pmAccountId)
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .firstName(firstName)
                .lastName(lastName)
                .email(request.getEmail())
                .phone(request.getPhone())
                .unitId(request.getUnitId())
                .idNumber(request.getIdNumber())
                .emergencyContact(request.getEmergencyContact())
                .emergencyPhone(request.getEmergencyPhone())
                .isActive(true)
                .creditBalance(0.0)
                .inviteStatus(InviteStatus.MANUAL)   // PM created directly — no invite
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant created (manual): {} for PM account {}", tenant.getId(), pmAccountId);
        return mapToDto(tenant);
    }

    @Transactional(readOnly = true)
    public TenantDto getTenant(UUID id, UUID pmAccountId) {
        Tenant tenant = tenantRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return mapToDto(tenant);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantDto> getTenants(UUID pmAccountId, int page, int size) {
        Page<Tenant> tenantPage = tenantRepository.findByPmAccountId(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(tenantPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantDto> getActiveTenants(UUID pmAccountId, int page, int size) {
        Page<Tenant> tenantPage = tenantRepository.findByPmAccountIdAndIsActiveTrue(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(tenantPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantDto> getTenantsByUnit(UUID unitId, UUID pmAccountId, int page, int size) {
        Page<Tenant> tenantPage = tenantRepository.findByUnitIdAndPmAccountId(
                unitId,
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(tenantPage);
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantByUserId(UUID userId) {
        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found for user"));
        return mapToDto(tenant);
    }

    @Transactional
    public TenantDto updateTenant(UUID id, UUID pmAccountId, TenantRequest request) {
        Tenant tenant = tenantRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            String[] nameParts = request.getFullName().trim().split("\\s+", 2);
            tenant.setFullName(request.getFullName());
            tenant.setFirstName(nameParts[0].isEmpty() ? "Unknown" : nameParts[0]);
            tenant.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            tenant.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            tenant.setEmail(request.getEmail());
        }
        if (request.getIdNumber() != null) {
            tenant.setIdNumber(request.getIdNumber());
        }
        if (request.getEmergencyContact() != null) {
            tenant.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getEmergencyPhone() != null) {
            tenant.setEmergencyPhone(request.getEmergencyPhone());
        }

        tenant = tenantRepository.save(tenant);
        log.info("Tenant updated: {}", id);
        return mapToDto(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id, UUID pmAccountId) {
        Tenant tenant = tenantRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        tenant.setIsActive(false);
        tenant.setDeletedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        log.info("Tenant deactivated: {}", id);
    }

    @Transactional
    public TenantDto updateTenantStatus(UUID id, UUID pmAccountId, boolean active) {
        Tenant tenant = tenantRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        tenant.setIsActive(active);
        if (!active) {
            tenant.setDeletedAt(LocalDateTime.now());
        }
        tenant = tenantRepository.save(tenant);
        return mapToDto(tenant);
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantInternal(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return mapToDto(tenant);
    }

    @Transactional(readOnly = true)
    public UUID getPmAccountForTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant::getPmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private TenantDto mapToDto(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .pmAccountId(tenant.getPmAccountId())
                .userId(tenant.getUserId())
                .fullName(tenant.getFullName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .unitId(tenant.getUnitId())
                .idNumber(tenant.getIdNumber())
                .emergencyContact(tenant.getEmergencyContact())
                .emergencyPhone(tenant.getEmergencyPhone())
                .isActive(tenant.getIsActive())
                .creditBalance(tenant.getCreditBalance())
                .inviteStatus(tenant.getInviteStatus())
                .invitedAt(tenant.getInvitedAt())
                .activatedAt(tenant.getActivatedAt())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    private PagedResponse<TenantDto> mapToPagedResponse(Page<Tenant> page) {
        List<TenantDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<TenantDto>builder()
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