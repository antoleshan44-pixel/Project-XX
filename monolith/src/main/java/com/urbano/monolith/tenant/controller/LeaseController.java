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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
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
    public ResponseEntity<LeaseDto> createLease(@Valid @RequestBody LeaseRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.ok(leaseService.createLease(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaseDto> getLease(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(leaseService.getLease(id, requireTenant()));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<LeaseDto>> getLeases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeases(requireTenant(), page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<LeaseDto>> getActiveLeases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getActiveLeases(requireTenant(), page, size));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeasesByTenant(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeasesByTenant(tenantId, requireTenant(), page, size));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeasesByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeasesByUnit(unitId, requireTenant(), page, size));
    }

    @GetMapping("/unit/{unitId}/active")
    public ResponseEntity<LeaseDto> getActiveLeaseByUnit(@PathVariable("unitId") UUID unitId) {
        return ResponseEntity.ok(leaseService.getActiveLeaseByUnit(unitId));
    }

    @GetMapping("/tenant/{tenantId}/active")
    public ResponseEntity<LeaseDto> getActiveLeaseByTenant(@PathVariable("tenantId") UUID tenantId) {
        return ResponseEntity.ok(leaseService.getActiveLeaseByTenant(tenantId));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<LeaseDto> terminateLease(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TerminateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.terminateLease(id, request));
    }
}