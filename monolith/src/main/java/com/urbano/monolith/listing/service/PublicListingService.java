package com.urbano.monolith.listing.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PropertyApprovalStatus;
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
     *   <li>v3 — Commit 7 added {@code unitPropertyType} + {@code transactionType}</li>
     *   <li>v4 — Commit 8 added {@code transactionType} as a query filter</li>
     *   <li><strong>v5 — Phase 2 (Model B): only units whose parent property has
     *       {@code approvalStatus = APPROVED} are returned. Cache key unchanged
     *       since the same inputs produce the same filtered output.</strong></li>
     * </ul>
     * Bumping the name means old cached entries are ignored and expire naturally.</p>
     */
    @Cacheable(value = "publicListingsV5",
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
                // ============================================================
                // Phase 2 (Model B) — platform-approval gate
                // A unit is only publicly visible if its parent property has
                // been APPROVED by a SUPER_ADMIN. Null means the property row
                // is missing or hasn't been fetched — exclude to be safe.
                // ============================================================
                .filter(u -> u.getPropertyApprovalStatus() == PropertyApprovalStatus.APPROVED)
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
                .filter(u -> propertyType == null ||
                        (u.getUnitPropertyType() != null
                                && u.getUnitPropertyType().name().equalsIgnoreCase(propertyType)))
                .filter(u -> transactionType == null
                        || transactionType == u.getTransactionType())
                .collect(Collectors.toList());

        return paginate(filtered, pageable);
    }

    /**
     * Single public listing by ID.
     *
     * <p>Phase 2 (Model B): only APPROVED properties are surfaced here. A
     * PENDING/REJECTED/SUSPENDED property 404s even if the caller knows the
     * UUID — the same guard as the list endpoint. Cache name bumped to v4.</p>
     */
    @Cacheable(value = "listingDetailsV4", key = "#id")
    public ListingDto getPublicListing(UUID id) {
        List<VacantUnitDto> units = unitService.getVacantPublishedUnitDtos();

        return units.stream()
                .filter(u -> u.getId().equals(id))
                // Phase 2 — approval gate
                .filter(u -> u.getPropertyApprovalStatus() == PropertyApprovalStatus.APPROVED)
                .findFirst()
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
    }

    /**
     * Submit an inquiry about a listing.
     * Uses the resolved PM display name (Commit 6).
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
                .propertyManagerPhone("+254 700 000 000")
                .propertyManagerEmail("manager@urbano.co.ke")
                .submittedAt(LocalDateTime.now())
                .build();
    }

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
                .propertyType(unit.getPropertyType())
                .squareFootage(unit.getSquareFootage())
                .unitPropertyType(unit.getUnitPropertyType())
                .transactionType(unit.getTransactionType())
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
    // Pagination helpers (unchanged)
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