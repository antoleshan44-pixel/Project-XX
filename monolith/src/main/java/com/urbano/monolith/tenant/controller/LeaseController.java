package com.urbano.monolith.tenant.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.tenant.dto.LeaseDto;
import com.urbano.monolith.tenant.dto.LeaseRequest;
import com.urbano.monolith.tenant.dto.TerminateLeaseRequest;
import com.urbano.monolith.tenant.service.LeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
public class LeaseController {

    private final LeaseService leaseService;

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<LeaseDto> createLease(@Valid @RequestBody LeaseRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.ok(leaseService.createLease(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaseDto> getLease(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(leaseService.getLease(id, TenantContext.getPmAccountId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeases(requireTenant(), page, size));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<LeaseDto>> getActiveLeases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getActiveLeases(requireTenant(), page, size));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeasesByTenant(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeasesByTenant(tenantId, TenantContext.getPmAccountId(), page, size));
    }

    @GetMapping("/unit/{unitId}")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeasesByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeasesByUnit(unitId, requireTenant(), page, size));
    }

    @GetMapping("/unit/{unitId}/active")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<LeaseDto> getActiveLeaseByUnit(@PathVariable("unitId") UUID unitId) {
        return ResponseEntity.ok(leaseService.getActiveLeaseByUnit(unitId));
    }

    @GetMapping("/tenant/{tenantId}/active")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
    public ResponseEntity<LeaseDto> getActiveLeaseByTenant(@PathVariable("tenantId") UUID tenantId) {
        return ResponseEntity.ok(leaseService.getActiveLeaseByTenant(tenantId));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LeaseDto> terminateLease(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TerminateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.terminateLease(id, request));
    }
}
