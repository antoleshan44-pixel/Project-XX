package com.urbano.monolith.property.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.ViewingStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.ValidationException;
import com.urbano.monolith.property.dto.ViewingDto;
import com.urbano.monolith.property.dto.ViewingRequest;
import com.urbano.monolith.property.dto.ViewingStatusUpdateRequest;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.entity.Viewing;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.property.repository.UnitRepository;
import com.urbano.monolith.property.repository.ViewingRepository;
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
public class ViewingService {

    private final ViewingRepository viewingRepository;
    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;

    // ============================================================
    // PUBLIC — create (no auth)
    // ============================================================
    @Transactional
    public ViewingDto createViewing(UUID unitId, ViewingRequest request) {
        // 1. Resolve the unit and its owning property so we can denormalize
        //    property_id and pm_account_id onto the viewing row for scoping.
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (unit.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Unit not found");
        }

        Property property = unit.getProperty();
        if (property == null) {
            // Should not happen — every unit has a NOT NULL FK to a property.
            throw new ResourceNotFoundException("Property for unit not found");
        }

        if (property.getPmAccountId() == null) {
            // Orphaned property — can't scope a viewing to a PM account.
            throw new ValidationException("Unit's property is not linked to a PM account");
        }

        // 2. Sanity check on the timestamp — @Future on the DTO already covers this,
        //    but keep a defensive runtime check in case the DTO is bypassed.
        if (request.getScheduledAt() == null || request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Scheduled date/time must be in the future");
        }

        Viewing viewing = Viewing.builder()
                .unitId(unit.getId())
                .propertyId(property.getId())
                .pmAccountId(property.getPmAccountId())
                .requestedByName(request.getRequestedByName())
                .requestedByPhone(request.getRequestedByPhone())
                .requestedByUserId(request.getRequestedByUserId())
                .scheduledAt(request.getScheduledAt())
                .status(ViewingStatus.SCHEDULED)
                .notes(request.getNotes())
                .build();

        viewing = viewingRepository.save(viewing);
        log.info("Viewing created: {} for unit {} (pmAccountId={})",
                viewing.getId(), unitId, property.getPmAccountId());
        return mapToDto(viewing);
    }

    // ============================================================
    // PM-SCOPED READS
    // ============================================================
    @Transactional(readOnly = true)
    public ViewingDto getViewing(UUID id, UUID pmAccountId) {
        Viewing viewing = viewingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewing not found"));
        return mapToDto(viewing);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViewingDto> getViewings(UUID pmAccountId, int page, int size) {
        Page<Viewing> viewingPage = viewingRepository.findByPmAccountId(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
        return mapToPagedResponse(viewingPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViewingDto> getViewingsByStatus(UUID pmAccountId, ViewingStatus status, int page, int size) {
        Page<Viewing> viewingPage = viewingRepository.findByPmAccountIdAndStatus(
                pmAccountId, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
        return mapToPagedResponse(viewingPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViewingDto> getViewingsByUnit(UUID unitId, UUID pmAccountId, int page, int size) {
        Page<Viewing> viewingPage = viewingRepository.findByUnitIdAndPmAccountId(
                unitId, pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
        return mapToPagedResponse(viewingPage);
    }

    // ============================================================
    // PM-SCOPED UPDATE
    // ============================================================
    @Transactional
    public ViewingDto updateStatus(UUID id, UUID pmAccountId, ViewingStatusUpdateRequest request) {
        Viewing viewing = viewingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewing not found"));

        ViewingStatus oldStatus = viewing.getStatus();
        viewing.setStatus(request.getStatus());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            viewing.setNotes(request.getNotes());
        }

        viewing = viewingRepository.save(viewing);
        log.info("Viewing {} status updated: {} -> {}", id, oldStatus, request.getStatus());
        return mapToDto(viewing);
    }

    // ============================================================
    // USER-SCOPED READ (future GET /users/{userId}/viewings)
    // ============================================================
    @Transactional(readOnly = true)
    public PagedResponse<ViewingDto> getViewingsForUser(UUID userId, int page, int size) {
        Page<Viewing> viewingPage = viewingRepository.findByRequestedByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
        return mapToPagedResponse(viewingPage);
    }

    // ============================================================
    // MAPPERS
    // ============================================================
    private ViewingDto mapToDto(Viewing viewing) {
        return ViewingDto.builder()
                .id(viewing.getId())
                .unitId(viewing.getUnitId())
                .propertyId(viewing.getPropertyId())
                .pmAccountId(viewing.getPmAccountId())
                .requestedByName(viewing.getRequestedByName())
                .requestedByPhone(viewing.getRequestedByPhone())
                .requestedByUserId(viewing.getRequestedByUserId())
                .scheduledAt(viewing.getScheduledAt())
                .status(viewing.getStatus())
                .notes(viewing.getNotes())
                .createdAt(viewing.getCreatedAt())
                .updatedAt(viewing.getUpdatedAt())
                .build();
    }

    private PagedResponse<ViewingDto> mapToPagedResponse(Page<Viewing> page) {
        List<ViewingDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<ViewingDto>builder()
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