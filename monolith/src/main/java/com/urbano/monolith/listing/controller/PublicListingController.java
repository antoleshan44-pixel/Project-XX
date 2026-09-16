package com.urbano.monolith.listing.controller;

import com.urbano.common.dto.PagedResponse;
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
     * PATCH 4: Get public listings with filters
     * No authentication required - public endpoint
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ListingDto>> getPublicListings(
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "bedrooms", required = false) Integer bedrooms,
            @RequestParam(value = "bathrooms", required = false) Integer bathrooms,  // ✅ Added
            @RequestParam(value = "propertyType", required = false) String propertyType,  // ✅ Added
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(publicListingService.getPublicListings(
                location, minPrice, maxPrice, bedrooms, bathrooms, propertyType, pageable
        ));
    }

    /**
     * Get a single public listing by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getPublicListing(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(publicListingService.getPublicListing(id));
    }

    /**
     * Submit an inquiry about a listing
     */
    @PostMapping("/{id}/inquire")
    public ResponseEntity<ListingInquiryResponse> inquire(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ListingInquiryRequest request) {
        return ResponseEntity.ok(publicListingService.submitInquiry(id, request));
    }
}