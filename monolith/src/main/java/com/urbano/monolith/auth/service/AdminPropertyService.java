package com.urbano.monolith.auth.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.audit.constants.AuditEventType;
import com.urbano.monolith.audit.dto.AuditLogRequest;
import com.urbano.monolith.audit.service.AuditService;
import com.urbano.monolith.property.dto.PropertyDto;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
public class AdminPropertyService {

    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<PropertyDto> listByApprovalStatus(
            PropertyApprovalStatus status, Pageable pageable) {
        Page<Property> page = (status == null)
                ? propertyRepository.findAll(pageable)
                : propertyRepository.findByApprovalStatus(status, pageable);
        return toPaged(page);
    }

    @Transactional
    @CacheEvict(value = {"publicListingsV5", "listingDetailsV4"}, allEntries = true)
    public PropertyDto approve(UUID propertyId, String reason) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (p.getApprovalStatus() == PropertyApprovalStatus.APPROVED) {
            return toDto(p); // idempotent
        }

        p.setApprovalStatus(PropertyApprovalStatus.APPROVED);
        p.setApprovedAt(LocalDateTime.now());
        p.setApprovedBy(TenantContext.getUserId());
        p.setRejectionReason(null);
        propertyRepository.save(p);

        audit("PROPERTY_APPROVED", propertyId, reason);
        log.info("[admin] Property {} APPROVED by {}", propertyId, TenantContext.getUserId());
        return toDto(p);
    }

    @Transactional
    @CacheEvict(value = {"publicListingsV5", "listingDetailsV4"}, allEntries = true)
    public PropertyDto reject(UUID propertyId, String reason) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (p.getApprovalStatus() == PropertyApprovalStatus.APPROVED) {
            throw new ConflictException(
                    "Property is APPROVED — use /suspend instead of /reject");
        }

        p.setApprovalStatus(PropertyApprovalStatus.REJECTED);
        p.setRejectionReason(reason);
        p.setApprovedAt(null);
        p.setApprovedBy(null);
        propertyRepository.save(p);

        audit("PROPERTY_REJECTED", propertyId, reason);
        log.info("[admin] Property {} REJECTED by {} — {}", propertyId, TenantContext.getUserId(), reason);
        return toDto(p);
    }

    @Transactional
    @CacheEvict(value = {"publicListingsV5", "listingDetailsV4"}, allEntries = true)
    public PropertyDto suspend(UUID propertyId, String reason) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (p.getApprovalStatus() != PropertyApprovalStatus.APPROVED) {
            throw new ConflictException("Only APPROVED properties can be suspended");
        }

        p.setApprovalStatus(PropertyApprovalStatus.SUSPENDED);
        p.setSuspendedReason(reason);
        propertyRepository.save(p);

        audit("PROPERTY_SUSPENDED", propertyId, reason);
        log.info("[admin] Property {} SUSPENDED by {}", propertyId, TenantContext.getUserId());
        return toDto(p);
    }

    @Transactional
    @CacheEvict(value = {"publicListingsV5", "listingDetailsV4"}, allEntries = true)
    public PropertyDto reactivate(UUID propertyId, String reason) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (p.getApprovalStatus() != PropertyApprovalStatus.SUSPENDED) {
            throw new ConflictException("Only SUSPENDED properties can be reactivated");
        }

        p.setApprovalStatus(PropertyApprovalStatus.APPROVED);
        p.setSuspendedReason(null);
        p.setApprovedAt(LocalDateTime.now());
        p.setApprovedBy(TenantContext.getUserId());
        propertyRepository.save(p);

        audit("PROPERTY_REACTIVATED", propertyId, reason);
        log.info("[admin] Property {} REACTIVATED by {}", propertyId, TenantContext.getUserId());
        return toDto(p);
    }

    private void audit(String action, UUID propertyId, String reason) {
        AuditLogRequest req = AuditLogRequest.builder()
                .eventType(AuditEventType.PROPERTY_STATUS_CHANGED)
                .userId(TenantContext.getUserId())
                .username("SUPER_ADMIN")
                .action(action)
                .resourceType("PROPERTY")
                .resourceId(propertyId)
                .metadata(java.util.Map.of("reason", reason == null ? "" : reason))
                .success(true)
                .serviceName("admin")
                .build();
        auditService.logAction(req);
    }

    private PropertyDto toDto(Property p) {
        return PropertyDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .address(p.getAddress())
                .city(p.getCity())
                .state(p.getState())
                .zipCode(p.getZipCode())
                .country(p.getCountry())
                .type(p.getType())
                .totalUnits(p.getTotalUnits())
                .status(p.getStatus())
                .ownerId(p.getOwnerId())
                .ownerName(p.getOwnerName())
                .ownerEmail(p.getOwnerEmail())
                .amenities(p.getAmenities())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .approvalStatus(p.getApprovalStatus())
                .approvedAt(p.getApprovedAt())
                .approvedBy(p.getApprovedBy())
                .rejectionReason(p.getRejectionReason())
                .build();
    }

    private PagedResponse<PropertyDto> toPaged(Page<Property> page) {
        List<PropertyDto> content = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return PagedResponse.<PropertyDto>builder()
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
