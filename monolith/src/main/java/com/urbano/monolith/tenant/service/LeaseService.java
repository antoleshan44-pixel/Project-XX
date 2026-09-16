package com.urbano.monolith.tenant.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.LeaseStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.property.service.UnitService;
import com.urbano.monolith.tenant.dto.LeaseDto;
import com.urbano.monolith.tenant.dto.LeaseRequest;
import com.urbano.monolith.tenant.dto.TerminateLeaseRequest;
import com.urbano.monolith.tenant.entity.Lease;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.LeaseRepository;
import com.urbano.monolith.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final UnitService unitService;

    @Transactional(rollbackFor = Exception.class)
    public LeaseDto createLease(LeaseRequest request) {
        log.info("Creating lease for tenant: {}, unit: {}", request.getTenantId(), request.getUnitId());

        Tenant tenant = tenantRepository.findByIdAndPmAccountId(
                        request.getTenantId(), request.getPmAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (leaseRepository.existsByTenantIdAndIsActiveTrue(request.getTenantId())) {
            throw new RuntimeException("Tenant already has an active lease");
        }

        if (leaseRepository.existsByUnitIdAndIsActiveTrue(request.getUnitId())) {
            throw new RuntimeException("Unit already has an active lease");
        }

        Lease lease = Lease.builder()
                .pmAccountId(request.getPmAccountId())
                .tenant(tenant)
                .tenantId(request.getTenantId())
                .unitId(request.getUnitId())
                .propertyId(request.getPropertyId())
                .startDate(request.getStartDate().atStartOfDay())
                .endDate(request.getEndDate().atStartOfDay())
                .rentAmount(BigDecimal.valueOf(request.getRentAmount()))
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .securityDeposit(request.getSecurityDeposit() != null ?
                        BigDecimal.valueOf(request.getSecurityDeposit()) : null)
                .paymentFrequency(request.getPaymentFrequency() != null ?
                        request.getPaymentFrequency() : "MONTHLY")
                .terms(request.getTerms())
                .status(LeaseStatus.ACTIVE)
                .isActive(true)
                .signedAt(LocalDateTime.now())
                .build();

        lease = leaseRepository.save(lease);
        log.info("Lease created: {}", lease.getId());

        try {
            unitService.occupyUnit(request.getUnitId());
            log.info("Unit {} marked as OCCUPIED", request.getUnitId());
        } catch (Exception e) {
            log.error("Failed to mark unit as OCCUPIED: {}", e.getMessage());
            throw new RuntimeException("Failed to update unit status", e);
        }

        tenant.setUnitId(request.getUnitId());
        tenantRepository.save(tenant);

        return mapToDto(lease);
    }

    @Transactional(rollbackFor = Exception.class)
    public LeaseDto terminateLease(UUID id, TerminateLeaseRequest request) {
        log.info("Terminating lease: {}", id);

        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        if (!lease.isCurrentlyActive()) {
            throw new RuntimeException("Lease is not active");
        }

        lease.setActive(false);
        lease.setStatus(LeaseStatus.TERMINATED);
        lease.setTerminatedAt(request.getTerminatedAt() != null ?
                request.getTerminatedAt() : LocalDateTime.now());
        lease.setTerminationReason(request.getReason());

        lease = leaseRepository.save(lease);
        log.info("Lease terminated: {}", id);

        try {
            unitService.vacateUnit(lease.getUnitId());
            log.info("Unit {} marked as VACANT", lease.getUnitId());
        } catch (Exception e) {
            log.error("Failed to mark unit as VACANT: {}", e.getMessage());
            throw new RuntimeException("Failed to update unit status", e);
        }

        Tenant tenant = lease.getTenant();
        if (tenant != null) {
            tenant.setUnitId(null);
            tenantRepository.save(tenant);
        }

        return mapToDto(lease);
    }

    @Transactional(readOnly = true)
    public LeaseDto getLease(UUID id, UUID pmAccountId) {
        Lease lease = leaseRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        return mapToDto(lease);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaseDto> getLeases(UUID pmAccountId, int page, int size) {
        Page<Lease> leasePage = leaseRepository.findByPmAccountId(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(leasePage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaseDto> getActiveLeases(UUID pmAccountId, int page, int size) {
        Page<Lease> leasePage = leaseRepository.findByPmAccountIdAndIsActiveTrue(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(leasePage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaseDto> getLeasesByTenant(UUID tenantId, UUID pmAccountId, int page, int size) {
        Page<Lease> leasePage = leaseRepository.findByTenantIdAndPmAccountId(
                tenantId,
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(leasePage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaseDto> getLeasesByUnit(UUID unitId, UUID pmAccountId, int page, int size) {
        Page<Lease> leasePage = leaseRepository.findByUnitIdAndPmAccountId(
                unitId,
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(leasePage);
    }

    @Transactional(readOnly = true)
    public LeaseDto getActiveLeaseByUnit(UUID unitId) {
        Lease lease = leaseRepository.findByUnitIdAndIsActiveTrue(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("No active lease found for unit: " + unitId));
        return mapToDto(lease);
    }

    @Transactional(readOnly = true)
    public LeaseDto getActiveLeaseByTenant(UUID tenantId) {
        Lease lease = leaseRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active lease found for tenant: " + tenantId));
        return mapToDto(lease);
    }

    public boolean isLeaseActive(UUID leaseId) {
        return leaseRepository.existsByIdAndIsActiveTrue(leaseId);
    }

    @Transactional
    public void updateTenantCreditBalance(UUID tenantId, Double amount) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        double newBalance = (tenant.getCreditBalance() != null ? tenant.getCreditBalance() : 0.0) + amount;
        tenant.setCreditBalance(newBalance);
        tenantRepository.save(tenant);
        log.info("Tenant credit balance updated: {} -> {}", tenantId, newBalance);
    }

    // ============================================================
    // MAPPER METHODS
    // ============================================================
    private LeaseDto mapToDto(Lease lease) {
        Tenant tenant = lease.getTenant();
        return LeaseDto.builder()
                .id(lease.getId())
                .pmAccountId(lease.getPmAccountId())
                .tenantId(lease.getTenantId())
                .tenantName(tenant != null ? tenant.getFullName() : null)
                .unitId(lease.getUnitId())
                .unitNumber(getUnitNumber(lease.getUnitId()))
                .propertyId(lease.getPropertyId())
                .propertyName(getPropertyName(lease.getPropertyId()))
                .startDate(lease.getStartDate())
                .endDate(lease.getEndDate())
                .rentAmount(lease.getRentAmount().doubleValue())
                .currency(lease.getCurrency())
                .securityDeposit(lease.getSecurityDeposit() != null ?
                        lease.getSecurityDeposit().doubleValue() : null)
                .paymentFrequency(lease.getPaymentFrequency())
                .status(lease.getStatus())
                .isActive(lease.isActive())
                .signedAt(lease.getSignedAt())
                .terminatedAt(lease.getTerminatedAt())
                .terminationReason(lease.getTerminationReason())
                .terms(lease.getTerms())
                .createdAt(lease.getCreatedAt())
                .updatedAt(lease.getUpdatedAt())
                .build();
    }

    private PagedResponse<LeaseDto> mapToPagedResponse(Page<Lease> page) {
        List<LeaseDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<LeaseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private String getUnitNumber(UUID unitId) {
        try {
            return unitService.getUnit(unitId).getUnitNumber();
        } catch (Exception e) {
            return null;
        }
    }

    private String getPropertyName(UUID propertyId) {
        return null;
    }
}