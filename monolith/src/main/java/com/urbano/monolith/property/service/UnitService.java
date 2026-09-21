package com.urbano.monolith.property.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.enums.UnitStatus;
import com.urbano.common.enums.UserRole;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.common.exception.ValidationException;
import com.urbano.common.storage.PhotoStorageService;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.property.dto.UnitDto;
import com.urbano.monolith.property.dto.UnitRequest;
import com.urbano.monolith.property.dto.VacantUnitDto;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
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

        // Commit 7: defaults for the two new fields if caller omitted them
        PropertyType propertyType = request.getPropertyType() != null
                ? request.getPropertyType() : PropertyType.APARTMENT;
        TransactionType transactionType = request.getTransactionType() != null
                ? request.getTransactionType() : TransactionType.FOR_RENT;

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
                .propertyType(propertyType)              // Commit 7
                .transactionType(transactionType)        // Commit 7
                .createdAt(LocalDateTime.now())
                .build();

        unit = unitRepository.save(unit);
        log.info("Unit created: {} for tenant {} (propertyType={}, transactionType={})",
                unit.getId(), pmAccountId, propertyType, transactionType);
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
        List<Unit> units = unitRepository.findByIsAvailableTrueAndPublishedTrue();
        Map<UUID, String> pmNames = resolvePmNames(units);
        return units.stream()
                .map(u -> mapToVacantUnitDto(u, pmNames))
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

        // Commit 7: PATCH semantics — only overwrite if caller provided
        if (request.getPropertyType() != null) {
            unit.setPropertyType(request.getPropertyType());
        }
        if (request.getTransactionType() != null) {
            unit.setTransactionType(request.getTransactionType());
        }

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

    /**
     * Commit 7: publish guard.
     * A unit cannot go live without a transactionType — the mobile UX review
     * flagged that a listing without one has an ambiguous price.
     */
    @Transactional
    public UnitDto publishUnit(UUID id) {
        Unit unit = requireOwnedUnit(id);
        if (unit.getTransactionType() == null) {
            throw new ValidationException(
                    "Cannot publish unit: transactionType is required (FOR_SALE or FOR_RENT)");
        }
        if (unit.getPropertyType() == null) {
            throw new ValidationException(
                    "Cannot publish unit: propertyType is required (APARTMENT, HOUSE, VILLA, STUDIO, PENTHOUSE)");
        }
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
            // Commit 7 hygiene: typed exception instead of RuntimeException
            throw new ValidationException("Invalid status: " + status);
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
    // PM NAME RESOLUTION (batch)
    // ============================================================
    private Map<UUID, String> resolvePmNames(List<Unit> units) {
        Set<UUID> pmAccountIds = units.stream()
                .map(Unit::getProperty)
                .filter(Objects::nonNull)
                .map(Property::getPmAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (pmAccountIds.isEmpty()) {
            return Map.of();
        }

        return userRepository
                .findByPmAccountIdInAndRole(pmAccountIds, UserRole.PM_ADMIN)
                .stream()
                .collect(Collectors.toMap(
                        User::getPmAccountId,
                        u -> (u.getFirstName() + " " + u.getLastName()).trim(),
                        (existing, replacement) -> existing
                ));
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
                .propertyType(unit.getPropertyType())           // Commit 7
                .transactionType(unit.getTransactionType())     // Commit 7
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }

    private VacantUnitDto mapToVacantUnitDto(Unit unit, Map<UUID, String> pmNames) {
        Property property = unit.getProperty();
        UUID pmAccountId = property != null ? property.getPmAccountId() : null;
        String pmName = pmAccountId != null ? pmNames.get(pmAccountId) : null;

        String thumbnail = (unit.getPhotoUrls() != null && !unit.getPhotoUrls().isEmpty())
                ? unit.getPhotoUrls().get(0)
                : null;

        return VacantUnitDto.builder()
                // unit-level
                .id(unit.getId())
                .propertyId(unit.getPropertyId())
                .unitNumber(unit.getUnitNumber())
                .label(unit.getLabel())
                .floor(unit.getFloor())
                .squareFootage(unit.getSquareFootage())
                .bedrooms(unit.getBedrooms())
                .bathrooms(unit.getBathrooms())
                .rentAmount(unit.getRentAmount() != null
                        ? BigDecimal.valueOf(unit.getRentAmount())
                        : null)
                .currency(unit.getCurrency())
                .description(unit.getDescription())
                .features(unit.getFeatures())
                .photoUrls(unit.getPhotoUrls() != null ? new ArrayList<>(unit.getPhotoUrls()) : new ArrayList<>())
                .thumbnailUrl(thumbnail)
                .status(unit.getStatus() != null ? unit.getStatus().name() : null)
                .published(unit.isPublished())
                // Commit 7 — unit-level filter fields
                .unitPropertyType(unit.getPropertyType())
                .transactionType(unit.getTransactionType())
                // property-level
                .propertyName(property != null ? property.getName() : null)
                .propertyType(property != null ? property.getType() : null)
                .address(property != null ? property.getAddress() : null)
                .city(property != null ? property.getCity() : null)
                .state(property != null ? property.getState() : null)
                .country(property != null ? property.getCountry() : null)
                .location(property != null ? property.getLocation() : null)
                // PM
                .pmAccountId(pmAccountId)
                .pmName(pmName)
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