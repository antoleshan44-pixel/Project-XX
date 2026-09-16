package com.urbano.monolith.listing.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.listing.dto.ListingDto;
import com.urbano.monolith.listing.dto.ListingRequest;
import com.urbano.monolith.listing.entity.Listing;
import com.urbano.monolith.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    @Transactional
    public ListingDto createListing(ListingRequest request) {
        // Check if listing already exists for this unit
        if (listingRepository.existsByUnitId(request.getUnitId())) {
            throw new RuntimeException("Listing already exists for this unit");
        }

        Listing listing = Listing.builder()
                .pmAccountId(request.getPmAccountId())
                .propertyId(request.getPropertyId())
                .unitId(request.getUnitId())
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .propertyType(request.getPropertyType())
                .squareFootage(request.getSquareFootage())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status("DRAFT")
                .published(false)
                .createdAt(LocalDateTime.now())
                .build();

        listing = listingRepository.save(listing);
        log.info("Listing created: {} for PM account {}", listing.getId(), listing.getPmAccountId());
        return mapToDto(listing);
    }

    public ListingDto getListing(UUID id, UUID pmAccountId) {
        Listing listing = listingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        return mapToDto(listing);
    }

    public PagedResponse<ListingDto> getAllListings(UUID pmAccountId, int page, int size) {
        Page<Listing> listingPage = listingRepository.findByPmAccountId(
                pmAccountId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return mapToPagedResponse(listingPage);
    }

    public PagedResponse<ListingDto> getListingsByProperty(UUID propertyId, UUID pmAccountId, int page, int size) {
        Page<Listing> listingPage = listingRepository.findByPropertyIdAndPmAccountId(
                propertyId, pmAccountId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return mapToPagedResponse(listingPage);
    }

    public PagedResponse<ListingDto> getActiveListings(UUID pmAccountId, int page, int size) {
        Page<Listing> listingPage = listingRepository.findByPmAccountIdAndStatus(
                pmAccountId, "ACTIVE", PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return mapToPagedResponse(listingPage);
    }

    @Transactional
    public ListingDto updateListing(UUID id, UUID pmAccountId, ListingRequest request) {
        Listing listing = listingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCurrency(request.getCurrency());
        listing.setBedrooms(request.getBedrooms());
        listing.setBathrooms(request.getBathrooms());
        listing.setPropertyType(request.getPropertyType());
        listing.setSquareFootage(request.getSquareFootage());
        listing.setAddress(request.getAddress());
        listing.setCity(request.getCity());
        listing.setState(request.getState());
        listing.setZipCode(request.getZipCode());
        listing.setCountry(request.getCountry());
        listing.setLatitude(request.getLatitude());
        listing.setLongitude(request.getLongitude());

        listing = listingRepository.save(listing);
        log.info("Listing updated: {}", listing.getId());
        return mapToDto(listing);
    }

    @Transactional
    public void deleteListing(UUID id, UUID pmAccountId) {
        Listing listing = listingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setStatus("DELETED");
        listing.setDeletedAt(LocalDateTime.now());
        listingRepository.save(listing);
        log.info("Listing deleted: {}", id);
    }

    @Transactional
    public ListingDto updateListingStatus(UUID id, UUID pmAccountId, String status) {
        Listing listing = listingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setStatus(status);
        listing = listingRepository.save(listing);
        return mapToDto(listing);
    }

    @Transactional
    public ListingDto publishListing(UUID id, UUID pmAccountId) {
        Listing listing = listingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setPublished(true);
        listing.setStatus("ACTIVE");
        listing = listingRepository.save(listing);
        return mapToDto(listing);
    }

    @Transactional
    public ListingDto unpublishListing(UUID id, UUID pmAccountId) {
        Listing listing = listingRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setPublished(false);
        listing.setStatus("DRAFT");
        listing = listingRepository.save(listing);
        return mapToDto(listing);
    }

    private ListingDto mapToDto(Listing listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .pmAccountId(listing.getPmAccountId())
                .propertyId(listing.getPropertyId())
                .unitId(listing.getUnitId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .currency(listing.getCurrency())
                .bedrooms(listing.getBedrooms())
                .bathrooms(listing.getBathrooms())
                .propertyType(listing.getPropertyType())
                .squareFootage(listing.getSquareFootage())
                .address(listing.getAddress())
                .city(listing.getCity())
                .state(listing.getState())
                .zipCode(listing.getZipCode())
                .country(listing.getCountry())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .status(listing.getStatus())
                .published(listing.isPublished())
                .photoUrls(listing.getPhotoUrls() != null ? List.of(listing.getPhotoUrls().split(",")) : List.of())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    private PagedResponse<ListingDto> mapToPagedResponse(Page<Listing> page) {
        List<ListingDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<ListingDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}