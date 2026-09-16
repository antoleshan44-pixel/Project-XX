package com.urbano.monolith.tenant.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.tenant.dto.TenantDto;
import com.urbano.monolith.tenant.dto.TenantRequest;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public TenantDto createTenant(TenantRequest request) {
        // Never trust client-supplied pmAccountId — must come from JWT
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
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant created: {} for PM account {}", tenant.getId(), tenant.getPmAccountId());
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

        // PATCH semantics: only update provided (non-null, non-blank) fields
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