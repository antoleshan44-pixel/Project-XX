package com.urbano.monolith.maintenance.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.monolith.maintenance.dto.*;
import com.urbano.monolith.maintenance.service.MaintenanceService;
import com.urbano.monolith.maintenance.service.PhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final PhotoService photoService;

    // ============================================================
    // CREATE - Scoped to PM Account
    // ============================================================
    @PostMapping
    public ResponseEntity<MaintenanceRequestDto> createRequest(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody MaintenanceRequestRequest request) {
        request.setPmAccountId(pmAccountId);
        return ResponseEntity.ok(maintenanceService.createRequest(request));
    }

    // ============================================================
    // GET BY ID - Scoped to PM Account
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRequestDto> getRequest(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(maintenanceService.getRequest(id, pmAccountId));
    }

    // ============================================================
    // GET ALL - Scoped to PM Account
    // ============================================================
    @GetMapping
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getAllRequests(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getAllRequests(pmAccountId, page, size));
    }

    // ============================================================
    // GET BY STATUS - Scoped to PM Account
    // ============================================================
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsByStatus(
            @PathVariable("status") MaintenanceStatus status,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsByStatus(pmAccountId, status, page, size));
    }

    // ============================================================
    // GET BY UNIT - Scoped to PM Account
    // ============================================================
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsByUnit(unitId, pmAccountId, page, size));
    }

    // ============================================================
    // GET BY PROPERTY - Scoped to PM Account
    // ============================================================
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsByProperty(
            @PathVariable("propertyId") UUID propertyId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsByProperty(propertyId, pmAccountId, page, size));
    }

    // ============================================================
    // UPDATE - Scoped to PM Account
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRequestDto> updateRequest(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody MaintenanceRequestRequest request) {
        return ResponseEntity.ok(maintenanceService.updateRequest(id, pmAccountId, request));
    }

    // ============================================================
    // UPDATE STATUS - Scoped to PM Account
    // ============================================================
    @PatchMapping("/{id}/status")
    public ResponseEntity<MaintenanceRequestDto> updateStatus(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(maintenanceService.updateStatus(id, pmAccountId, request));
    }

    // ============================================================
    // ASSIGN - Scoped to PM Account
    // ============================================================
    @PostMapping("/{id}/assign")
    public ResponseEntity<MaintenanceRequestDto> assignRequest(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam("assignedTo") UUID assignedTo) {
        return ResponseEntity.ok(maintenanceService.assignRequest(id, pmAccountId, assignedTo));
    }

    // ============================================================
    // PHOTO UPLOAD URL - Scoped to PM Account
    // ============================================================
    @PostMapping("/{id}/photos/upload-url")
    public ResponseEntity<PhotoUploadUrlResponse> getPhotoUploadUrl(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam("fileName") String fileName) {
        return ResponseEntity.ok(photoService.getPhotoUploadUrl(id, pmAccountId, fileName));
    }

    // ============================================================
    // ADD PHOTO - Scoped to PM Account
    // ============================================================
    @PostMapping("/{id}/photos")
    public ResponseEntity<MaintenanceRequestDto> addPhoto(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam("photoUrl") String photoUrl) {
        return ResponseEntity.ok(maintenanceService.addPhoto(id, pmAccountId, photoUrl));
    }

    // ============================================================
    // DELETE (Soft Delete) - Scoped to PM Account
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        maintenanceService.deleteRequest(id, pmAccountId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // GET REQUESTS FOR TENANT - Scoped to Tenant
    // ============================================================
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsForTenant(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsForTenant(tenantId, page, size));
    }
}