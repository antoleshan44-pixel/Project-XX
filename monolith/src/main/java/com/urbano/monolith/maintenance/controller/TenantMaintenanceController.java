package com.urbano.monolith.maintenance.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.maintenance.dto.MaintenanceRequestDto;
import com.urbano.monolith.maintenance.dto.MaintenanceRequestRequest;
import com.urbano.monolith.maintenance.service.MaintenanceService;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT', 'SUPER_ADMIN')")
public class TenantMaintenanceController {

    private final MaintenanceService maintenanceService;
    private final TenantRepository tenantRepository;

    private void verifyTenantAccess(UUID tenantId) {
        if (TenantContext.isAdminBypass()) {
            return;
        }
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new UnauthorizedException("No authenticated user");
        }
        if (userId.equals(tenantId)) {
            return;
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new UnauthorizedException("Tenant not found"));
        if (!userId.equals(tenant.getUserId())) {
            throw new UnauthorizedException("Tenant ID does not match authenticated user");
        }
    }

    @PostMapping
    public ResponseEntity<MaintenanceRequestDto> createRequest(@Valid @RequestBody MaintenanceRequestRequest request) {
        if (request.getTenantId() != null) {
            verifyTenantAccess(request.getTenantId());
        }
        return ResponseEntity.ok(maintenanceService.createRequest(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getTenantRequests(
            @RequestParam("tenantId") UUID tenantId, Pageable pageable) {
        verifyTenantAccess(tenantId);
        return ResponseEntity.ok(maintenanceService.getTenantRequests(tenantId, pageable));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<MaintenanceRequestDto> getTenantRequest(
            @RequestParam("tenantId") UUID tenantId, @PathVariable("requestId") UUID requestId) {
        verifyTenantAccess(tenantId);
        return ResponseEntity.ok(maintenanceService.getTenantRequest(tenantId, requestId));
    }
}
