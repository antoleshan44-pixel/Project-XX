package com.urbano.monolith.maintenance.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.maintenance.dto.MaintenanceRequestDto;
import com.urbano.monolith.maintenance.dto.MaintenanceRequestRequest;
import com.urbano.monolith.maintenance.dto.StatusUpdateRequest;
import com.urbano.monolith.maintenance.entity.MaintenanceRequest;
import com.urbano.monolith.maintenance.repository.MaintenanceRepository;
import com.urbano.monolith.property.service.PropertyService;
import com.urbano.monolith.property.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final UnitService unitService;
    private final PropertyService propertyService;

    @Transactional
    public MaintenanceRequestDto createRequest(MaintenanceRequestRequest request) {
        UUID pmAccountId = com.urbano.common.context.TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context");
        }

        // Verify the unit belongs to this tenant
        if (!unitService.validateUnitPmAccount(request.getUnitId(), pmAccountId)) {
            throw new UnauthorizedException("Unit does not belong to this PM account");
        }

        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .pmAccountId(pmAccountId)
                .propertyId(request.getPropertyId())
                .unitId(request.getUnitId())
                .tenantId(request.getTenantId())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .status(MaintenanceStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        maintenance = maintenanceRepository.save(maintenance);
        log.info("Maintenance request created: {} for PM account {}", maintenance.getId(), pmAccountId);
        return mapToDto(maintenance);
    }

    @Transactional(readOnly = true)
    public MaintenanceRequestDto getRequest(UUID id, UUID pmAccountId) {
        MaintenanceRequest maintenance = maintenanceRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));
        return mapToDto(maintenance);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceRequestDto> getAllRequests(UUID pmAccountId, int page, int size) {
        Page<MaintenanceRequest> pageable = maintenanceRepository.findByPmAccountId(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceRequestDto> getRequestsByStatus(
            UUID pmAccountId,
            MaintenanceStatus status,
            int page,
            int size) {
        Page<MaintenanceRequest> pageable = maintenanceRepository.findByPmAccountIdAndStatus(
                pmAccountId,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceRequestDto> getRequestsByUnit(
            UUID unitId,
            UUID pmAccountId,
            int page,
            int size) {
        Page<MaintenanceRequest> pageable = maintenanceRepository.findByUnitIdAndPmAccountId(
                unitId,
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceRequestDto> getRequestsByProperty(
            UUID propertyId,
            UUID pmAccountId,
            int page,
            int size) {
        Page<MaintenanceRequest> pageable = maintenanceRepository.findByPropertyIdAndPmAccountId(
                propertyId,
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(pageable);
    }

    @Transactional
    public MaintenanceRequestDto updateRequest(UUID id, UUID pmAccountId, MaintenanceRequestRequest request) {
        MaintenanceRequest maintenance = maintenanceRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));

        maintenance.setTitle(request.getTitle());
        maintenance.setDescription(request.getDescription());
        maintenance.setPriority(request.getPriority());

        maintenance = maintenanceRepository.save(maintenance);
        log.info("Maintenance request updated: {}", maintenance.getId());
        return mapToDto(maintenance);
    }

    @Transactional
    public MaintenanceRequestDto updateStatus(UUID id, UUID pmAccountId, StatusUpdateRequest request) {
        MaintenanceRequest maintenance = maintenanceRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));

        MaintenanceStatus oldStatus = maintenance.getStatus();
        maintenance.setStatus(request.getStatus());
        maintenance.setNotes(request.getNotes());

        if (request.getStatus() == MaintenanceStatus.RESOLVED) {
            maintenance.setResolvedAt(LocalDateTime.now());
        } else if (request.getStatus() == MaintenanceStatus.CLOSED) {
            maintenance.setCompletedDate(LocalDateTime.now());
        } else if (request.getStatus() == MaintenanceStatus.CANCELLED) {
            maintenance.setDeletedAt(LocalDateTime.now());
        }

        if (oldStatus == MaintenanceStatus.OPEN &&
                request.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            maintenance.setScheduledDate(LocalDateTime.now());
        }

        maintenance = maintenanceRepository.save(maintenance);
        log.info("Maintenance request status updated: {} -> {} for request {}",
                oldStatus, request.getStatus(), id);

        return mapToDto(maintenance);
    }

    @Transactional
    public MaintenanceRequestDto assignRequest(UUID id, UUID pmAccountId, UUID assignedTo) {
        MaintenanceRequest maintenance = maintenanceRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));

        maintenance.setAssignedTo(assignedTo);
        if (maintenance.getStatus() == MaintenanceStatus.OPEN) {
            maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        }

        maintenance = maintenanceRepository.save(maintenance);
        log.info("Maintenance request assigned: {} -> {}", id, assignedTo);

        return mapToDto(maintenance);
    }

    @Transactional
    public MaintenanceRequestDto addPhoto(UUID id, UUID pmAccountId, String photoUrl) {
        MaintenanceRequest maintenance = maintenanceRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));

        if (maintenance.getPhotoUrl() == null) {
            maintenance.setPhotoUrl(photoUrl);
        }
        String existing = maintenance.getPhotoUrls();
        if (existing == null || existing.isEmpty()) {
            maintenance.setPhotoUrls(photoUrl);
        } else {
            maintenance.setPhotoUrls(existing + "," + photoUrl);
        }

        maintenance = maintenanceRepository.save(maintenance);
        log.info("Photo added to maintenance request: {}", id);
        return mapToDto(maintenance);
    }

    @Transactional
    public void deleteRequest(UUID id, UUID pmAccountId) {
        MaintenanceRequest maintenance = maintenanceRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));

        maintenance.setStatus(MaintenanceStatus.CANCELLED);
        maintenance.setDeletedAt(LocalDateTime.now());
        maintenanceRepository.save(maintenance);
        log.info("Maintenance request cancelled: {}", id);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceRequestDto> getRequestsForTenant(UUID tenantId, int page, int size) {
        Page<MaintenanceRequest> pageable = maintenanceRepository.findByTenantId(
                tenantId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceRequestDto> getTenantRequests(UUID tenantId, Pageable pageable) {
        Page<MaintenanceRequest> page = maintenanceRepository.findByTenantId(tenantId, pageable);
        List<MaintenanceRequestDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<MaintenanceRequestDto>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public MaintenanceRequestDto getTenantRequest(UUID tenantId, UUID requestId) {
        MaintenanceRequest request = maintenanceRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found for tenant: " + tenantId));
        return mapToDto(request);
    }

    // ============================================================
    // MAPPER METHODS
    // ============================================================
    private MaintenanceRequestDto mapToDto(MaintenanceRequest maintenance) {
        return MaintenanceRequestDto.builder()
                .id(maintenance.getId())
                .pmAccountId(maintenance.getPmAccountId())
                .propertyId(maintenance.getPropertyId())
                .propertyName(getPropertyName(maintenance.getPropertyId()))
                .unitId(maintenance.getUnitId())
                .unitNumber(getUnitNumber(maintenance.getUnitId()))
                .tenantId(maintenance.getTenantId())
                .tenantName(getTenantName(maintenance.getTenantId()))
                .title(maintenance.getTitle())
                .description(maintenance.getDescription())
                .priority(maintenance.getPriority())
                .status(maintenance.getStatus())
                .assignedTo(maintenance.getAssignedTo())
                .assignedToName(getAssignedToName(maintenance.getAssignedTo()))
                .scheduledDate(maintenance.getScheduledDate())
                .completedDate(maintenance.getCompletedDate())
                .resolvedAt(maintenance.getResolvedAt())
                .notes(maintenance.getNotes())
                .photoUrl(maintenance.getPhotoUrl())
                .photoUrls(maintenance.getPhotoUrls() != null ?
                        List.of(maintenance.getPhotoUrls().split(",")) : List.of())
                .createdAt(maintenance.getCreatedAt())
                .updatedAt(maintenance.getUpdatedAt())
                .build();
    }

    private PagedResponse<MaintenanceRequestDto> mapToPagedResponse(Page<MaintenanceRequest> page) {
        List<MaintenanceRequestDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<MaintenanceRequestDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getPropertyName(UUID propertyId) {
        try {
            return propertyService.getProperty(propertyId).getName();
        } catch (Exception e) {
            log.warn("Failed to get property name for {}: {}", propertyId, e.getMessage());
            return null;
        }
    }

    private String getUnitNumber(UUID unitId) {
        try {
            return unitService.getUnit(unitId).getUnitNumber();
        } catch (Exception e) {
            log.warn("Failed to get unit number for {}: {}", unitId, e.getMessage());
            return null;
        }
    }

    private String getTenantName(UUID tenantId) {
        return null;
    }

    private String getAssignedToName(UUID assignedTo) {
        return null;
    }
}