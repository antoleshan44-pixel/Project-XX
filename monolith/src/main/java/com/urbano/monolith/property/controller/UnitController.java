package com.urbano.monolith.property.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.property.dto.UnitDto;
import com.urbano.monolith.property.dto.UnitRequest;
import com.urbano.monolith.property.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    public ResponseEntity<UnitDto> createUnit(@Valid @RequestBody UnitRequest request) {
        return ResponseEntity.ok(unitService.createUnit(request));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<PagedResponse<UnitDto>> getUnitsByProperty(
            @PathVariable("propertyId") UUID propertyId, Pageable pageable) {
        return ResponseEntity.ok(unitService.getUnits(propertyId, pageable));
    }

    @GetMapping("/vacant")
    public ResponseEntity<?> getVacantUnits() {
        return ResponseEntity.ok(unitService.getVacantPublishedUnits());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitDto> getUnit(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(unitService.getUnit(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitDto> updateUnit(
            @PathVariable("id") UUID id, @Valid @RequestBody UnitRequest request) {
        return ResponseEntity.ok(unitService.updateUnit(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable("id") UUID id) {
        unitService.deleteUnit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<UnitDto> publishUnit(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(unitService.publishUnit(id));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<UnitDto> unpublishUnit(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(unitService.unpublishUnit(id));
    }

    @PostMapping("/{id}/photos/upload-url")
    public ResponseEntity<String> getPhotoUploadUrl(
            @PathVariable("id") UUID id, @RequestParam("fileName") String fileName) {
        return ResponseEntity.ok(unitService.getPhotoUploadUrl(id, fileName));
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<UnitDto> addPhoto(
            @PathVariable("id") UUID id, @RequestParam("photoUrl") String photoUrl) {
        return ResponseEntity.ok(unitService.addPhoto(id, photoUrl));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UnitDto> updateStatus(
            @PathVariable("id") UUID id, @RequestParam("status") String status) {
        return ResponseEntity.ok(unitService.updateStatus(id, status));
    }
}
