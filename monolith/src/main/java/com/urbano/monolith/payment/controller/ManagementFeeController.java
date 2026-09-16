package com.urbano.monolith.payment.controller;

import com.urbano.monolith.payment.dto.ManagementFeeRequest;
import com.urbano.monolith.payment.dto.ManagementFeeResponse;
import com.urbano.monolith.payment.service.ManagementFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/management-fees")
@RequiredArgsConstructor
public class ManagementFeeController {

    private final ManagementFeeService managementFeeService;

    @PostMapping
    public ResponseEntity<ManagementFeeResponse> generateFee(@Valid @RequestBody ManagementFeeRequest request) {
        // TODO: Get pmAccountId from authentication context
        UUID pmAccountId = UUID.randomUUID(); // Temporary - should come from security context
        log.info("Generating management fee for property: {}", request.getPropertyId());
        ManagementFeeResponse response = managementFeeService.generateFee(request, pmAccountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ManagementFeeResponse>> getManagementFees(Pageable pageable) {
        UUID pmAccountId = UUID.randomUUID(); // Temporary - should come from security context
        Page<ManagementFeeResponse> response = managementFeeService.getManagementFees(pmAccountId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<ManagementFeeResponse> markAsPaid(@PathVariable("id") UUID id) {
        UUID pmAccountId = UUID.randomUUID(); // Temporary - should come from security context
        ManagementFeeResponse response = managementFeeService.markAsPaid(id, pmAccountId);
        return ResponseEntity.ok(response);
    }
}