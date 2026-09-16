package com.urbano.monolith.maintenance.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.maintenance.dto.MaintenanceRequestDto;
import com.urbano.monolith.maintenance.dto.MaintenanceRequestRequest;
import com.urbano.monolith.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/maintenance")
@RequiredArgsConstructor
public class TenantMaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceRequestDto> createRequest(@Valid @RequestBody MaintenanceRequestRequest request) {
        return ResponseEntity.ok(maintenanceService.createRequest(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<MaintenanceRequestDto>> getTenantRequests(
            @RequestParam("tenantId") UUID tenantId, Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.getTenantRequests(tenantId, pageable));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<MaintenanceRequestDto> getTenantRequest(
            @RequestParam("tenantId") UUID tenantId, @PathVariable("requestId") UUID requestId) {
        return ResponseEntity.ok(maintenanceService.getTenantRequest(tenantId, requestId));
    }
}
