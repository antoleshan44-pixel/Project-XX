package com.urbano.monolith.tenant.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
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

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody TenantRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.ok(tenantService.createTenant(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantDto> getTenant(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(tenantService.getTenant(id, requireTenant()));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TenantDto>> getTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.getTenants(requireTenant(), page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<TenantDto>> getActiveTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.getActiveTenants(requireTenant(), page, size));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<PagedResponse<TenantDto>> getTenantsByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.getTenantsByUnit(unitId, requireTenant(), page, size));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<TenantDto> getTenantByUserId(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(tenantService.getTenantByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TenantDto> updateTenant(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, requireTenant(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TenantDto> patchTenant(
            @PathVariable("id") UUID id,
            @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, requireTenant(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable("id") UUID id) {
        tenantService.deleteTenant(id, requireTenant());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantDto> updateTenantStatus(
            @PathVariable("id") UUID id,
            @RequestParam("active") boolean active) {
        return ResponseEntity.ok(tenantService.updateTenantStatus(id, requireTenant(), active));
    }
}