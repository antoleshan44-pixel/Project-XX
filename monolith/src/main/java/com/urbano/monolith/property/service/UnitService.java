package com.urbano.monolith.property.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.UnitStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.common.storage.PhotoStorageService;
import com.urbano.monolith.listing.dto.VacantUnitDto;
import com.urbano.monolith.property.dto.UnitDto;
import com.urbano.monolith.property.dto.UnitRequest;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.property.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    private final PhotoStorageService photoStorageService;

    // ============================================================
    // Tenant helpers
    // ============================================================
    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    private Unit requireOwnedUnit(UUID id) {
        UUID pmAccountId = requireTenant();
        return unitRepository.findByIdAndTenant(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
    }

    // ============================================================
    // CREATE
    // ============================================================
    @Transactional
    public UnitDto createUnit(UnitRequest request) {
        UUID pmAccountId = requireTenant();

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getPmAccountId() == null
                || !pmAccountId.equals(property.getPmAccountId())) {
            throw new UnauthorizedException("Access denied to this property");
        }

        Unit unit = Unit.builder()
                .property(property)
                .propertyId(property.getId())
                .unitNumber(request.getUnitNumber())
                .floor(request.getFloor() != null ? request.getFloor() : 1)
                .squareFootage(request.getSquareFootage())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .rentAmount(request.getRentAmount() != null ? request.getRentAmount().doubleValue() : 0.0)
                .currency(request.getCurrency())
                .isAvailable(true)
                .status(UnitStatus.AVAILABLE)
                .description(request.getDescription())
                .features(request.getFeatures())
                .published(true)
                .label("Unit " + request.getUnitNumber())
                .photoUrls(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        unit = unitRepository.save(unit);
        log.info("Unit created: {} for tenant {}", unit.getId(), pmAccountId);
        return mapToDto(unit);
    }

    // ============================================================
    // READ
    // ============================================================
    @Transactional(readOnly = true)
    public UnitDto getUnit(UUID id) {
        return mapToDto(requireOwnedUnit(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UnitDto> getUnits(UUID propertyId, Pageable pageable) {
        UUID pmAccountId = requireTenant();

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getPmAccountId() == null
                || !pmAccountId.equals(property.getPmAccountId())) {
            throw new UnauthorizedException("Access denied to this property");
        }

        Page<Unit> unitPage = unitRepository.findByPropertyIdAndTenant(propertyId, pmAccountId, pageable);
        return toPagedResponse(unitPage, pageable);
    }

    /** Public — no auth. */
    @Transactional(readOnly = true)
    public List<UnitDto> getVacantPublishedUnits() {
        return unitRepository.findByIsAvailableTrueAndPublishedTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VacantUnitDto> getVacantPublishedUnitDtos() {
        return unitRepository.findByIsAvailableTrueAndPublishedTrue().stream()
                .map(this::mapToVacantUnitDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UPDATE
    // ============================================================
    @Transactional
    public UnitDto updateUnit(UUID id, UnitRequest request) {
        Unit unit = requireOwnedUnit(id);

        unit.setUnitNumber(request.getUnitNumber());
        unit.setFloor(request.getFloor());
        unit.setSquareFootage(request.getSquareFootage());
        unit.setBedrooms(request.getBedrooms());
        unit.setBathrooms(request.getBathrooms());
        unit.setRentAmount(request.getRentAmount() != null ? request.getRentAmount().doubleValue() : 0.0);
        unit.setCurrency(request.getCurrency());
        unit.setDescription(request.getDescription());
        unit.setFeatures(request.getFeatures());

        unit = unitRepository.save(unit);
        log.info("Unit updated: {}", unit.getId());
        return mapToDto(unit);
    }

    @Transactional
    public void deleteUnit(UUID id) {
        Unit unit = requireOwnedUnit(id);
        unit.setDeletedAt(LocalDateTime.now());
        unitRepository.save(unit);
        log.info("Unit deleted: {}", id);
    }

    @Transactional
    public UnitDto publishUnit(UUID id) {
        Unit unit = requireOwnedUnit(id);
        unit.setPublished(true);
        unit = unitRepository.save(unit);
        return mapToDto(unit);
    }

    @Transactional
    public UnitDto unpublishUnit(UUID id) {
        Unit unit = requireOwnedUnit(id);
        unit.setPublished(false);
        unit = unitRepository.save(unit);
        return mapToDto(unit);
    }

    @Transactional
    public UnitDto addPhoto(UUID unitId, String photoUrl) {
        Unit unit = requireOwnedUnit(unitId);
        if (unit.getPhotoUrls() == null) {
            unit.setPhotoUrls(new ArrayList<>());
        }
        unit.getPhotoUrls().add(photoUrl);
        unit = unitRepository.save(unit);
        return mapToDto(unit);
    }

    @Transactional
    public UnitDto updateStatus(UUID id, String status) {
        Unit unit = requireOwnedUnit(id);
        try {
            UnitStatus newStatus = UnitStatus.valueOf(status.toUpperCase());
            unit.setStatus(newStatus);
            unit.setIsAvailable(newStatus == UnitStatus.AVAILABLE);
            unit = unitRepository.save(unit);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
        return mapToDto(unit);
    }

    public String getPhotoUploadUrl(UUID unitId, String fileName) {
        requireOwnedUnit(unitId);
        String key = "units/" + unitId + "/" + fileName;
        return photoStorageService.uploadFile(key, new byte[0], "image/jpeg");
    }

    // ============================================================
    // INTERNAL — called by LeaseService/MaintenanceService/TenantService
    // These bypass TenantContext and operate on the raw unit ID.
    // ============================================================

    @Transactional
    public void occupyUnit(UUID unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unit.setStatus(UnitStatus.RENTED);
        unit.setIsAvailable(false);
        unit.setRentedAt(LocalDateTime.now());
        unit.setPublished(false);
        unitRepository.save(unit);
        log.info("Unit occupied: {}", unitId);
    }

    @Transactional
    public void vacateUnit(UUID unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unit.setStatus(UnitStatus.AVAILABLE);
        unit.setIsAvailable(true);
        unit.setCurrentTenantId(null);
        unit.setRentedAt(null);
        unitRepository.save(unit);
        log.info("Unit vacated: {}", unitId);
    }

    @Transactional
    public void setUnitUnderMaintenance(UUID unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unit.setStatus(UnitStatus.UNDER_MAINTENANCE);
        unit.setIsAvailable(false);
        unitRepository.save(unit);
        log.info("Unit under maintenance: {}", unitId);
    }

    @Transactional(readOnly = true)
    public boolean validateUnitPmAccount(UUID unitId, UUID pmAccountId) {
        return unitRepository.existsByIdAndTenant(unitId, pmAccountId);
    }

    // ============================================================
    // MAPPERS
    // ============================================================
    private UnitDto mapToDto(Unit unit) {
        return UnitDto.builder()
                .id(unit.getId())
                .propertyId(unit.getPropertyId())
                .unitNumber(unit.getUnitNumber())
                .floor(unit.getFloor())
                .squareFootage(unit.getSquareFootage())
                .bedrooms(unit.getBedrooms())
                .bathrooms(unit.getBathrooms())
                .rentAmount(unit.getRentAmount())
                .currency(unit.getCurrency())
                .isAvailable(unit.getIsAvailable())
                .published(unit.isPublished())
                .status(unit.getStatus())
                .description(unit.getDescription())
                .features(unit.getFeatures())
                .photoUrls(unit.getPhotoUrls() != null ? new ArrayList<>(unit.getPhotoUrls()) : new ArrayList<>())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }

    private VacantUnitDto mapToVacantUnitDto(Unit unit) {
        Property property = unit.getProperty();
        String thumbnail = null;
        if (unit.getPhotoUrls() != null && !unit.getPhotoUrls().isEmpty()) {
            thumbnail = unit.getPhotoUrls().get(0);
        }

        return VacantUnitDto.builder()
                .id(unit.getId())
                .propertyId(unit.getPropertyId())
                .propertyName(property != null ? property.getName() : null)
                .unitNumber(unit.getUnitNumber())
                .rentAmount(unit.getRentAmount())
                .currency(unit.getCurrency())
                .bedrooms(unit.getBedrooms())
                .bathrooms(unit.getBathrooms())
                .propertyType(property != null ? property.getType() : null)
                .squareFootage(unit.getSquareFootage())
                .address(property != null ? property.getAddress() : null)
                .city(property != null ? property.getCity() : null)
                .state(property != null ? property.getState() : null)
                .description(unit.getDescription())
                .thumbnailUrl(thumbnail)
                .status(unit.getStatus() != null ? unit.getStatus().name() : null)
                .published(unit.isPublished())
                .build();
    }

    private PagedResponse<UnitDto> toPagedResponse(Page<Unit> page, Pageable pageable) {
        List<UnitDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<UnitDto>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}