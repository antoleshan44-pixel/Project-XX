package com.urbano.monolith.listing.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.TransactionType;
import com.urbano.monolith.listing.dto.ListingDto;
import com.urbano.monolith.listing.dto.ListingInquiryRequest;
import com.urbano.monolith.listing.dto.ListingInquiryResponse;
import com.urbano.monolith.listing.service.PublicListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/public/listings")
@RequiredArgsConstructor
public class PublicListingController {

    private final PublicListingService publicListingService;

    /**
     * Public listings with filters. No authentication required.
     *
     * <p>Filter params:</p>
     * <ul>
     *   <li>{@code location} — substring match on address/city/state</li>
     *   <li>{@code minPrice}, {@code maxPrice} — rent range</li>
     *   <li>{@code bedrooms}, {@code bathrooms} — minimums</li>
     *   <li>{@code propertyType} — unit-level enum
     *       (APARTMENT/HOUSE/VILLA/STUDIO/PENTHOUSE). Commit 7.</li>
     *   <li>{@code transactionType} — FOR_SALE/FOR_RENT. Commit 8.</li>
     * </ul>
     *
     * <p>An invalid enum value in {@code transactionType} returns 400 via
     * Spring's built-in request-param conversion.</p>
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ListingDto>> getPublicListings(
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "bedrooms", required = false) Integer bedrooms,
            @RequestParam(value = "bathrooms", required = false) Integer bathrooms,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "transactionType", required = false) TransactionType transactionType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(publicListingService.getPublicListings(
                location, minPrice, maxPrice, bedrooms, bathrooms,
                propertyType, transactionType, pageable
        ));
    }

    /**
     * Get a single public listing by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getPublicListing(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(publicListingService.getPublicListing(id));
    }

    /**
     * Submit an inquiry about a listing.
     */
    @PostMapping("/{id}/inquire")
    public ResponseEntity<ListingInquiryResponse> inquire(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ListingInquiryRequest request) {
        return ResponseEntity.ok(publicListingService.submitInquiry(id, request));
    }
}