package com.urbano.monolith.listing.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.listing.dto.ListingDto;
import com.urbano.monolith.listing.dto.ListingInquiryRequest;
import com.urbano.monolith.listing.dto.ListingInquiryResponse;
import com.urbano.monolith.listing.dto.VacantUnitDto;
import com.urbano.monolith.property.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicListingService {

    private final UnitService unitService;

    /**
     * PATCH 4: Get public listings with caching
     * Cache key includes all filter parameters for proper cache isolation
     */
    @Cacheable(value = "publicListings",
            key = "{#location, #minPrice, #maxPrice, #bedrooms, #bathrooms, #propertyType, #pageable.pageNumber, #pageable.pageSize}")
    public PagedResponse<ListingDto> getPublicListings(
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            String propertyType,
            Pageable pageable) {

        log.info("Fetching public listings with filters: location={}, minPrice={}, maxPrice={}, " +
                        "bedrooms={}, bathrooms={}, propertyType={}",
                location, minPrice, maxPrice, bedrooms, bathrooms, propertyType);

        // Fetch vacant+published units with flattened property info from the property module
        List<VacantUnitDto> units = unitService.getVacantPublishedUnitDtos();

        if (units == null || units.isEmpty()) {
            return PagedResponse.<ListingDto>builder()
                    .content(List.of())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        // Apply additional filters locally
        List<VacantUnitDto> filtered = units.stream()
                .filter(u -> location == null ||
                        (u.getAddress() != null && u.getAddress().toLowerCase().contains(location.toLowerCase())) ||
                        (u.getCity() != null && u.getCity().toLowerCase().contains(location.toLowerCase())) ||
                        (u.getState() != null && u.getState().toLowerCase().contains(location.toLowerCase())))
                .filter(u -> minPrice == null || u.getRentAmount().compareTo(minPrice.doubleValue()) >= 0)
                .filter(u -> maxPrice == null || u.getRentAmount().compareTo(maxPrice.doubleValue()) <= 0)
                .filter(u -> bedrooms == null || u.getBedrooms() >= bedrooms)
                .filter(u -> bathrooms == null || u.getBathrooms() >= bathrooms)
                .filter(u -> propertyType == null ||
                        (u.getPropertyType() != null && u.getPropertyType().equalsIgnoreCase(propertyType)))
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        if (start >= filtered.size()) {
            return PagedResponse.<ListingDto>builder()
                    .content(List.of())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements((long) filtered.size())
                    .totalPages((int) Math.ceil((double) filtered.size() / pageable.getPageSize()))
                    .first(pageable.getPageNumber() == 0)
                    .last(true)
                    .build();
        }

        List<ListingDto> content = filtered.subList(start, end).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<ListingDto>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements((long) filtered.size())
                .totalPages((int) Math.ceil((double) filtered.size() / pageable.getPageSize()))
                .first(pageable.getPageNumber() == 0)
                .last((long) end >= filtered.size())
                .build();
    }

    /**
     * Get a single public listing by ID
     */
    @Cacheable(value = "listingDetails", key = "#id")
    public ListingDto getPublicListing(UUID id) {
        List<VacantUnitDto> units = unitService.getVacantPublishedUnitDtos();

        return units.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
    }

    /**
     * Submit an inquiry about a listing
     */
    public ListingInquiryResponse submitInquiry(UUID listingId, ListingInquiryRequest request) {
        ListingDto listing = getPublicListing(listingId);

        log.info("Inquiry received for listing {} from {} ({})",
                listingId, request.getName(), request.getEmail());

        return ListingInquiryResponse.builder()
                .inquiryId(UUID.randomUUID())
                .status("SUBMITTED")
                .message("Your inquiry has been submitted. The property manager will contact you shortly.")
                .propertyManagerName("Property Manager")
                .propertyManagerPhone("+254 700 000 000")
                .propertyManagerEmail("manager@urbano.co.ke")
                .submittedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Map VacantUnitDto to ListingDto
     */
    private ListingDto mapToDto(VacantUnitDto unit) {
        return ListingDto.builder()
                .id(unit.getId())
                .propertyId(unit.getPropertyId())
                .unitId(unit.getId())
                .title(unit.getPropertyName() + " - " + unit.getUnitNumber())
                .description(unit.getDescription())
                .price(unit.getRentAmount())
                .currency(unit.getCurrency())
                .bedrooms(unit.getBedrooms())
                .bathrooms(unit.getBathrooms())
                .propertyType(unit.getPropertyType())
                .squareFootage(unit.getSquareFootage())
                .address(unit.getAddress())
                .city(unit.getCity())
                .state(unit.getState())
                .status(unit.getStatus())
                .published(unit.isPublished())
                .photoUrls(unit.getThumbnailUrl() != null ? List.of(unit.getThumbnailUrl()) : List.of())
                .build();
    }
}