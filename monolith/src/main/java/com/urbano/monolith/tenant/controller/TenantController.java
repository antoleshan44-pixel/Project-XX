package com.urbano.monolith.tenant.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.tenant.dto.TenantDto;
import com.urbano.monolith.tenant.dto.TenantRequest;
import com.urbano.monolith.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantDto> createTenant(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody TenantRequest request) {
        request.setPmAccountId(pmAccountId);
        return ResponseEntity.ok(tenantService.createTenant(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantDto> getTenant(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(tenantService.getTenant(id, pmAccountId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TenantDto>> getTenants(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.getTenants(pmAccountId, page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<TenantDto>> getActiveTenants(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.getActiveTenants(pmAccountId, page, size));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<PagedResponse<TenantDto>> getTenantsByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.getTenantsByUnit(unitId, pmAccountId, page, size));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<TenantDto> getTenantByUserId(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(tenantService.getTenantByUserId(userId));
    }

    /**
     * PUT — full update (backwards compatible)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TenantDto> updateTenant(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, pmAccountId, request));
    }

    /**
     * ✅ NEW — PATCH for partial updates
     * Used by the E2E test which sends only { phone: "..." }
     * The service applies null-safe PATCH semantics internally.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TenantDto> patchTenant(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, pmAccountId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        tenantService.deleteTenant(id, pmAccountId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantDto> updateTenantStatus(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam("active") boolean active) {
        return ResponseEntity.ok(tenantService.updateTenantStatus(id, pmAccountId, active));
    }
}