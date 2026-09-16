package com.urbano.monolith.property.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.UnitStatus;
import com.urbano.common.exception.ResourceNotFoundException;
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

    @Transactional
    public UnitDto createUnit(UnitRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

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
        log.info("Unit created: {}", unit.getId());
        return mapToDto(unit);
    }

    @Transactional(readOnly = true)
    public UnitDto getUnit(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        return mapToDto(unit);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UnitDto> getUnits(UUID propertyId, Pageable pageable) {
        Page<Unit> unitPage = unitRepository.findByPropertyId(propertyId, pageable);
        List<UnitDto> content = unitPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<UnitDto>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(unitPage.getTotalElements())
                .totalPages(unitPage.getTotalPages())
                .first(unitPage.isFirst())
                .last(unitPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UnitDto> getVacantPublishedUnits() {
        return unitRepository.findByIsAvailableTrueAndPublishedTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Returns vacant+published units enriched with their property's flat fields
     * (address, city, state, name, type) for the public listing view.
     * Runs inside a read-only transaction so lazy-loading Property is safe.
     */
    @Transactional(readOnly = true)
    public List<VacantUnitDto> getVacantPublishedUnitDtos() {
        return unitRepository.findByIsAvailableTrueAndPublishedTrue().stream()
                .map(this::mapToVacantUnitDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UnitDto updateUnit(UUID id, UnitRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

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
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unit.setDeletedAt(LocalDateTime.now());
        unitRepository.save(unit);
        log.info("Unit deleted: {}", id);
    }

    @Transactional
    public UnitDto publishUnit(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unit.setPublished(true);
        unit = unitRepository.save(unit);
        return mapToDto(unit);
    }

    @Transactional
    public UnitDto unpublishUnit(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        unit.setPublished(false);
        unit = unitRepository.save(unit);
        return mapToDto(unit);
    }

    public String getPhotoUploadUrl(UUID unitId, String fileName) {
        String key = "units/" + unitId + "/" + fileName;
        return photoStorageService.uploadFile(key, new byte[0], "image/jpeg");
    }

    @Transactional
    public UnitDto addPhoto(UUID unitId, String photoUrl) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        if (unit.getPhotoUrls() == null) {
            unit.setPhotoUrls(new ArrayList<>());
        }
        unit.getPhotoUrls().add(photoUrl);
        unit = unitRepository.save(unit);
        return mapToDto(unit);
    }

    @Transactional
    public UnitDto updateStatus(UUID id, String status) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
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

    // ============================================================
    // INTERNAL METHODS (called by tenant-service and maintenance-service)
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
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        if (unit.getProperty() == null) {
            return false;
        }
        return pmAccountId.equals(unit.getProperty().getPmAccountId());
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
}