package com.urbano.monolith.payment.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.payment.dto.ManagementFeeRequest;
import com.urbano.monolith.payment.dto.ManagementFeeResponse;
import com.urbano.monolith.payment.service.ManagementFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/management-fees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PM_ADMIN', 'SUPER_ADMIN')")
public class ManagementFeeController {

    private final ManagementFeeService managementFeeService;

    private UUID requirePmAccountId() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null && !TenantContext.isAdminBypass()) {
            throw new UnauthorizedException("No PM account context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    public ResponseEntity<ManagementFeeResponse> generateFee(@Valid @RequestBody ManagementFeeRequest request) {
        UUID pmAccountId = requirePmAccountId();
        log.info("Generating management fee for property: {}", request.getPropertyId());
        ManagementFeeResponse response = managementFeeService.generateFee(request, pmAccountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ManagementFeeResponse>> getManagementFees(Pageable pageable) {
        UUID pmAccountId = requirePmAccountId();
        Page<ManagementFeeResponse> response = managementFeeService.getManagementFees(pmAccountId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<ManagementFeeResponse> markAsPaid(@PathVariable("id") UUID id) {
        UUID pmAccountId = requirePmAccountId();
        ManagementFeeResponse response = managementFeeService.markAsPaid(id, pmAccountId);
        return ResponseEntity.ok(response);
    }
}
