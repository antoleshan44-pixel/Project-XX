package com.urbano.monolith.tenant.controller;

import com.urbano.common.dto.PagedResponse;
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

    @PostMapping
    public ResponseEntity<LeaseDto> createLease(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody LeaseRequest request) {
        request.setPmAccountId(pmAccountId);
        return ResponseEntity.ok(leaseService.createLease(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaseDto> getLease(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(leaseService.getLease(id, pmAccountId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<LeaseDto>> getLeases(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeases(pmAccountId, page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<LeaseDto>> getActiveLeases(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getActiveLeases(pmAccountId, page, size));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeasesByTenant(
            @PathVariable("tenantId") UUID tenantId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeasesByTenant(tenantId, pmAccountId, page, size));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<PagedResponse<LeaseDto>> getLeasesByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaseService.getLeasesByUnit(unitId, pmAccountId, page, size));
    }

    @GetMapping("/unit/{unitId}/active")
    public ResponseEntity<LeaseDto> getActiveLeaseByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(leaseService.getActiveLeaseByUnit(unitId));
    }

    @GetMapping("/tenant/{tenantId}/active")
    public ResponseEntity<LeaseDto> getActiveLeaseByTenant(
            @PathVariable("tenantId") UUID tenantId) {
        return ResponseEntity.ok(leaseService.getActiveLeaseByTenant(tenantId));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<LeaseDto> terminateLease(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody TerminateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.terminateLease(id, request));
    }
}