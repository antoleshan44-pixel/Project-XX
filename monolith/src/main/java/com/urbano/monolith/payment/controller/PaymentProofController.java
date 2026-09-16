package com.urbano.monolith.payment.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.payment.dto.PaymentProofDto;
import com.urbano.monolith.payment.dto.PaymentProofRequest;
import com.urbano.monolith.payment.dto.PaymentProofResolveRequest;
import com.urbano.monolith.payment.entity.PaymentProof;
import com.urbano.monolith.payment.service.PaymentProofService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentProofController {

    private final PaymentProofService paymentProofService;

    /**
     * PATCH 7: Tenant submits payment proof
     */
    @PostMapping("/api/tenants/{tenantId}/payments/proof")
    public ResponseEntity<PaymentProofDto> submitPaymentProof(
            @PathVariable("tenantId") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PaymentProofRequest request) {
        // Verify tenant matches authenticated user
        if (!tenantId.equals(userId)) {
            throw new SecurityException("Tenant ID does not match authenticated user");
        }
        return ResponseEntity.ok(paymentProofService.submitPaymentProof(tenantId, request));
    }

    /**
     * PATCH 7: Get pending payment proofs for PM review
     */
    @GetMapping("/api/payments/proofs/pending")
    public ResponseEntity<PagedResponse<PaymentProofDto>> getPendingProofs(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentProofService.getPendingProofs(pmAccountId, page, size));
    }

    /**
     * PATCH 7: Resolve a payment proof (match or reject)
     */
    @PutMapping("/api/payments/proofs/{id}/resolve")
    public ResponseEntity<PaymentProofDto> resolveProof(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody PaymentProofResolveRequest request) {
        return ResponseEntity.ok(paymentProofService.resolveProof(id, pmAccountId, request));
    }

    /**
     * PATCH 7: Get proof by ID
     */
    @GetMapping("/api/payments/proofs/{id}")
    public ResponseEntity<PaymentProofDto> getProof(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(paymentProofService.getProof(id, pmAccountId));
    }

    /**
     * PATCH 7: Get proofs for a tenant
     */
    @GetMapping("/api/tenants/{tenantId}/payments/proofs")
    public ResponseEntity<PagedResponse<PaymentProofDto>> getTenantProofs(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentProofService.getTenantProofs(tenantId, page, size));
    }
}