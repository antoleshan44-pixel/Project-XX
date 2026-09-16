package com.urbano.monolith.listing.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
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

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    public ResponseEntity<ListingDto> createListing(@Valid @RequestBody ListingRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.ok(listingService.createListing(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getListing(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(listingService.getListing(id, requireTenant()));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ListingDto>> getAllListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getAllListings(requireTenant(), page, size));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<PagedResponse<ListingDto>> getListingsByProperty(
            @PathVariable("propertyId") UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getListingsByProperty(propertyId, requireTenant(), page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<ListingDto>> getActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getActiveListings(requireTenant(), page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingDto> updateListing(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ListingRequest request) {
        return ResponseEntity.ok(listingService.updateListing(id, requireTenant(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable("id") UUID id) {
        listingService.deleteListing(id, requireTenant());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingDto> updateListingStatus(
            @PathVariable("id") UUID id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(listingService.updateListingStatus(id, requireTenant(), status));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ListingDto> publishListing(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(listingService.publishListing(id, requireTenant()));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ListingDto> unpublishListing(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(listingService.unpublishListing(id, requireTenant()));
    }
}