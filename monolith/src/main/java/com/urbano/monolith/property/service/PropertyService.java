package com.urbano.monolith.property.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PropertyStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.common.exception.ValidationException;
import com.urbano.monolith.property.dto.PropertyDto;
import com.urbano.monolith.property.dto.PropertyRequest;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Transactional
    public PropertyDto createProperty(PropertyRequest request) {
        UUID pmAccountId = requireTenant();

        // Owner defaults to the creating user (a PM_ADMIN) if the client did not
        // supply one. The `properties.owner_id` column is NOT NULL, so we must
        // always have a value here.
        UUID ownerId = request.getOwnerId() != null
                ? request.getOwnerId()
                : TenantContext.getUserId();

        if (ownerId == null) {
            throw new ValidationException(
                    "Cannot create property: ownerId is required and no authenticated user is present");
        }

        Property property = Property.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .type(request.getType())
                .totalUnits(request.getTotalUnits())
                .status(PropertyStatus.AVAILABLE)
                .ownerId(ownerId)
                .ownerName(request.getOwnerName())
                .ownerEmail(request.getOwnerEmail())
                .amenities(request.getAmenities())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(request.getAddress() + ", " + request.getCity())
                .pmAccountId(pmAccountId)
                .createdAt(LocalDateTime.now())
                .build();

        property = propertyRepository.save(property);
        log.info("Property created: {} for tenant {} (ownerId={})",
                property.getId(), pmAccountId, ownerId);
        return mapToDto(property);
    }

    public PropertyDto getProperty(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        assertTenantOwns(property);
        return mapToDto(property);
    }

    public PagedResponse<PropertyDto> getProperties(Pageable pageable) {
        UUID pmAccountId = requireTenant();
        Page<Property> propertyPage = propertyRepository.findByPmAccountId(pmAccountId, pageable);
        return toPagedResponse(propertyPage, pageable);
    }

    public PagedResponse<PropertyDto> getPropertiesByOwner(UUID ownerId, Pageable pageable) {
        UUID pmAccountId = requireTenant();
        Page<Property> propertyPage = propertyRepository.findByOwnerIdAndPmAccountId(ownerId, pmAccountId, pageable);
        return toPagedResponse(propertyPage, pageable);
    }

    @Transactional
    public PropertyDto updateProperty(UUID id, PropertyRequest request) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        assertTenantOwns(property);

        property.setName(request.getName());
        property.setDescription(request.getDescription());
        property.setAddress(request.getAddress());
        property.setCity(request.getCity());
        property.setState(request.getState());
        property.setZipCode(request.getZipCode());
        property.setCountry(request.getCountry());
        property.setType(request.getType());
        property.setTotalUnits(request.getTotalUnits());
        property.setAmenities(request.getAmenities());
        property.setLatitude(request.getLatitude());
        property.setLongitude(request.getLongitude());
        property.setLocation(request.getAddress() + ", " + request.getCity());

        property = propertyRepository.save(property);
        log.info("Property updated: {}", property.getId());
        return mapToDto(property);
    }

    @Transactional
    public void deleteProperty(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        assertTenantOwns(property);
        property.setDeletedAt(LocalDateTime.now());
        propertyRepository.save(property);
        log.info("Property deleted: {}", id);
    }

    @Transactional
    public PropertyDto updatePropertyStatus(UUID id, PropertyStatus status) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        assertTenantOwns(property);
        property.setStatus(status);
        property = propertyRepository.save(property);
        return mapToDto(property);
    }

    // -----------------------------------------------------------------
    // Tenant guards
    // -----------------------------------------------------------------

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    private void assertTenantOwns(Property property) {
        UUID pmAccountId = requireTenant();
        if (property.getPmAccountId() == null
                || !pmAccountId.equals(property.getPmAccountId())) {
            throw new UnauthorizedException("Access denied to this property");
        }
    }

    private PagedResponse<PropertyDto> toPagedResponse(Page<Property> page, Pageable pageable) {
        List<PropertyDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<PropertyDto>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private PropertyDto mapToDto(Property property) {
        return PropertyDto.builder()
                .id(property.getId())
                .name(property.getName())
                .description(property.getDescription())
                .address(property.getAddress())
                .city(property.getCity())
                .state(property.getState())
                .zipCode(property.getZipCode())
                .country(property.getCountry())
                .type(property.getType())
                .totalUnits(property.getTotalUnits())
                .status(property.getStatus())
                .ownerId(property.getOwnerId())
                .ownerName(property.getOwnerName())
                .ownerEmail(property.getOwnerEmail())
                .amenities(property.getAmenities())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
}