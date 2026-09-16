package com.urbano.monolith.property.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PropertyStatus;
import com.urbano.monolith.property.dto.PropertyDto;
import com.urbano.monolith.property.dto.PropertyRequest;
import com.urbano.monolith.property.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<PropertyDto> createProperty(@Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.ok(propertyService.createProperty(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PropertyDto>> getProperties(Pageable pageable) {
        return ResponseEntity.ok(propertyService.getProperties(pageable));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<PagedResponse<PropertyDto>> getPropertiesByOwner(
            @PathVariable("ownerId") UUID ownerId, Pageable pageable) {
        return ResponseEntity.ok(propertyService.getPropertiesByOwner(ownerId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDto> getProperty(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(propertyService.getProperty(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyDto> updateProperty(
            @PathVariable("id") UUID id, @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.ok(propertyService.updateProperty(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable("id") UUID id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PropertyDto> updatePropertyStatus(
            @PathVariable("id") UUID id, @RequestParam("status") PropertyStatus status) {
        return ResponseEntity.ok(propertyService.updatePropertyStatus(id, status));
    }
}
