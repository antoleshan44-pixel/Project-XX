package com.urbano.monolith.listing.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.listing.dto.ListingDto;
import com.urbano.monolith.listing.dto.ListingInquiryRequest;
import com.urbano.monolith.listing.dto.ListingInquiryResponse;
import com.urbano.monolith.property.dto.VacantUnitDto;
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
     * Public listings with caching.
     *
     * <p>Cache name history:
     * <ul>
     *   <li>v1 — original</li>
     *   <li>v2 — Commit 6 added {@code pmName}</li>
     *   <li>v3 — Commit 7 added {@code unitPropertyType} + {@code transactionType}
     *       to the response payload</li>
     *   <li>v4 — Commit 8 added {@code transactionType} as a query filter</li>
     * </ul>
     * Bumping the name means old cached entries are ignored and expire naturally.
     * The key now includes {@code #transactionType} so different filter combinations
     * cannot collide on the same cache entry.</p>
     */
    @Cacheable(value = "publicListingsV4",
            key = "{#location, #minPrice, #maxPrice, #bedrooms, #bathrooms, #propertyType, #transactionType, #pageable.pageNumber, #pageable.pageSize}")
    public PagedResponse<ListingDto> getPublicListings(
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            String propertyType,
            TransactionType transactionType,
            Pageable pageable) {

        log.info("Fetching public listings with filters: location={}, minPrice={}, maxPrice={}, " +
                        "bedrooms={}, bathrooms={}, propertyType={}, transactionType={}",
                location, minPrice, maxPrice, bedrooms, bathrooms, propertyType, transactionType);

        List<VacantUnitDto> units = unitService.getVacantPublishedUnitDtos();

        if (units == null || units.isEmpty()) {
            return emptyPage(pageable);
        }

        List<VacantUnitDto> filtered = units.stream()
                .filter(u -> location == null ||
                        (u.getAddress() != null && u.getAddress().toLowerCase().contains(location.toLowerCase())) ||
                        (u.getCity() != null && u.getCity().toLowerCase().contains(location.toLowerCase())) ||
                        (u.getState() != null && u.getState().toLowerCase().contains(location.toLowerCase())))
                .filter(u -> minPrice == null || u.getRentAmount() == null
                        || u.getRentAmount().compareTo(minPrice) >= 0)
                .filter(u -> maxPrice == null || u.getRentAmount() == null
                        || u.getRentAmount().compareTo(maxPrice) <= 0)
                .filter(u -> bedrooms == null || (u.getBedrooms() != null && u.getBedrooms() >= bedrooms))
                .filter(u -> bathrooms == null || (u.getBathrooms() != null && u.getBathrooms() >= bathrooms))
                // Commit 7 — propertyType filter on the unit-level enum
                .filter(u -> propertyType == null ||
                        (u.getUnitPropertyType() != null
                                && u.getUnitPropertyType().name().equalsIgnoreCase(propertyType)))
                // Commit 8 — transactionType filter (enum identity comparison; enums are singletons)
                .filter(u -> transactionType == null
                        || transactionType == u.getTransactionType())
                .collect(Collectors.toList());

        return paginate(filtered, pageable);
    }

    /**
     * Single public listing by ID.
     * Cache name bumped to {@code listingDetailsV3} in Commit 7.
     */
    @Cacheable(value = "listingDetailsV3", key = "#id")
    public ListingDto getPublicListing(UUID id) {
        List<VacantUnitDto> units = unitService.getVacantPublishedUnitDtos();

        return units.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
    }

    /**
     * Submit an inquiry about a listing.
     * Uses the resolved PM display name (Commit 6). Phone and email are
     * placeholders — wiring them requires a decision on whether the PM
     * consents to contact info being surfaced publicly.
     */
    public ListingInquiryResponse submitInquiry(UUID listingId, ListingInquiryRequest request) {
        ListingDto listing = getPublicListing(listingId);

        String pmName = listing.getPmName() != null
                ? listing.getPmName()
                : "Property Manager";

        log.info("Inquiry received for listing {} from {} ({})",
                listingId, request.getName(), request.getEmail());

        return ListingInquiryResponse.builder()
                .inquiryId(UUID.randomUUID())
                .status("SUBMITTED")
                .message("Your inquiry has been submitted. The property manager will contact you shortly.")
                .propertyManagerName(pmName)
                .propertyManagerPhone("+254 700 000 000")   // TODO: resolve real contact if consented
                .propertyManagerEmail("manager@urbano.co.ke") // TODO: same
                .submittedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Map VacantUnitDto (property module) to ListingDto (listing module).
     *
     * <p>Two "type" fields travel through:
     * <ul>
     *   <li>{@code unitPropertyType} — enum, unit-level</li>
     *   <li>{@code propertyType} — String, property-level</li>
     * </ul>
     */
    private ListingDto mapToDto(VacantUnitDto unit) {
        return ListingDto.builder()
                .id(unit.getId())
                .pmAccountId(unit.getPmAccountId())
                .propertyId(unit.getPropertyId())
                .unitId(unit.getId())
                .title(unit.getPropertyName() + " - " + unit.getUnitNumber())
                .description(unit.getDescription())
                .price(unit.getRentAmount() != null ? unit.getRentAmount().doubleValue() : null)
                .currency(unit.getCurrency())
                .bedrooms(unit.getBedrooms())
                .bathrooms(unit.getBathrooms())
                .propertyType(unit.getPropertyType())           // property-level String
                .squareFootage(unit.getSquareFootage())
                .unitPropertyType(unit.getUnitPropertyType())   // Commit 7 — unit-level enum
                .transactionType(unit.getTransactionType())     // Commit 7
                .address(unit.getAddress())
                .city(unit.getCity())
                .state(unit.getState())
                .status(unit.getStatus())
                .published(unit.isPublished())
                .photoUrls(unit.getThumbnailUrl() != null
                        ? List.of(unit.getThumbnailUrl())
                        : List.of())
                .pmName(unit.getPmName())
                .build();
    }

    // ============================================================
    // Pagination helpers
    // ============================================================
    private PagedResponse<ListingDto> emptyPage(Pageable pageable) {
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

    private PagedResponse<ListingDto> paginate(List<VacantUnitDto> filtered, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        int totalPages = (int) Math.ceil((double) filtered.size() / pageable.getPageSize());

        if (start >= filtered.size()) {
            return PagedResponse.<ListingDto>builder()
                    .content(List.of())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements((long) filtered.size())
                    .totalPages(totalPages)
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
                .totalPages(totalPages)
                .first(pageable.getPageNumber() == 0)
                .last((long) end >= filtered.size())
                .build();
    }
}