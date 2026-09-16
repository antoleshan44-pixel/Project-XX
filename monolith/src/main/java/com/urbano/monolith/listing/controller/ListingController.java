package com.urbano.monolith.listing.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.listing.dto.ListingDto;
import com.urbano.monolith.listing.dto.ListingRequest;
import com.urbano.monolith.listing.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    public ResponseEntity<ListingDto> createListing(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody ListingRequest request) {
        // ✅ Ensure PM Account from header matches request
        request.setPmAccountId(pmAccountId);
        return ResponseEntity.ok(listingService.createListing(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getListing(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(listingService.getListing(id, pmAccountId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ListingDto>> getAllListings(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getAllListings(pmAccountId, page, size));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<PagedResponse<ListingDto>> getListingsByProperty(
            @PathVariable("propertyId") UUID propertyId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getListingsByProperty(propertyId, pmAccountId, page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<ListingDto>> getActiveListings(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getActiveListings(pmAccountId, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingDto> updateListing(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody ListingRequest request) {
        return ResponseEntity.ok(listingService.updateListing(id, pmAccountId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        listingService.deleteListing(id, pmAccountId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingDto> updateListingStatus(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(listingService.updateListingStatus(id, pmAccountId, status));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ListingDto> publishListing(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(listingService.publishListing(id, pmAccountId));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ListingDto> unpublishListing(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(listingService.unpublishListing(id, pmAccountId));
    }
}