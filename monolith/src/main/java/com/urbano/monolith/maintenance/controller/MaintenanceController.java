package com.urbano.monolith.maintenance.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.common.exception.UnauthorizedException;
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

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    // ============================================================
    // CREATE - Scoped to PM Account (from JWT)
    // ============================================================
    @PostMapping
    public ResponseEntity<MaintenanceRequestDto> createRequest(
            @Valid @RequestBody MaintenanceRequestRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.ok(maintenanceService.createRequest(request));
    }

    // ============================================================
    // GET BY ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRequestDto> getRequest(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(maintenanceService.getRequest(id, requireTenant()));
    }

    // ============================================================
    // GET ALL
    // ============================================================
    @GetMapping
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getAllRequests(requireTenant(), page, size));
    }

    // ============================================================
    // GET BY STATUS
    // ============================================================
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsByStatus(
            @PathVariable("status") MaintenanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsByStatus(requireTenant(), status, page, size));
    }

    // ============================================================
    // GET BY UNIT
    // ============================================================
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsByUnit(unitId, requireTenant(), page, size));
    }

    // ============================================================
    // GET BY PROPERTY
    // ============================================================
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsByProperty(
            @PathVariable("propertyId") UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsByProperty(propertyId, requireTenant(), page, size));
    }

    // ============================================================
    // UPDATE
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRequestDto> updateRequest(
            @PathVariable("id") UUID id,
            @Valid @RequestBody MaintenanceRequestRequest request) {
        return ResponseEntity.ok(maintenanceService.updateRequest(id, requireTenant(), request));
    }

    // ============================================================
    // UPDATE STATUS
    // ============================================================
    @PatchMapping("/{id}/status")
    public ResponseEntity<MaintenanceRequestDto> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(maintenanceService.updateStatus(id, requireTenant(), request));
    }

    // ============================================================
    // ASSIGN
    // ============================================================
    @PostMapping("/{id}/assign")
    public ResponseEntity<MaintenanceRequestDto> assignRequest(
            @PathVariable("id") UUID id,
            @RequestParam("assignedTo") UUID assignedTo) {
        return ResponseEntity.ok(maintenanceService.assignRequest(id, requireTenant(), assignedTo));
    }

    // ============================================================
    // PHOTO UPLOAD URL
    // ============================================================
    @PostMapping("/{id}/photos/upload-url")
    public ResponseEntity<PhotoUploadUrlResponse> getPhotoUploadUrl(
            @PathVariable("id") UUID id,
            @RequestParam("fileName") String fileName) {
        return ResponseEntity.ok(photoService.getPhotoUploadUrl(id, requireTenant(), fileName));
    }

    // ============================================================
    // ADD PHOTO
    // ============================================================
    @PostMapping("/{id}/photos")
    public ResponseEntity<MaintenanceRequestDto> addPhoto(
            @PathVariable("id") UUID id,
            @RequestParam("photoUrl") String photoUrl) {
        return ResponseEntity.ok(maintenanceService.addPhoto(id, requireTenant(), photoUrl));
    }

    // ============================================================
    // DELETE (Soft Delete)
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable("id") UUID id) {
        maintenanceService.deleteRequest(id, requireTenant());
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // GET REQUESTS FOR TENANT
    // ============================================================
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getRequestsForTenant(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(maintenanceService.getRequestsForTenant(tenantId, page, size));
    }
}