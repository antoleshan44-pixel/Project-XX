package com.urbano.monolith.payment.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.payment.dto.PaymentProofDto;
import com.urbano.monolith.payment.dto.PaymentProofRequest;
import com.urbano.monolith.payment.dto.PaymentProofResolveRequest;
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

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    private UUID requireUserId() {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new UnauthorizedException("No user context — authentication required");
        }
        return userId;
    }

    /**
     * PATCH 7: Tenant submits payment proof.
     * The tenantId from the URL must match the authenticated user from the JWT.
     */
    @PostMapping("/api/tenants/{tenantId}/payments/proof")
    public ResponseEntity<PaymentProofDto> submitPaymentProof(
            @PathVariable("tenantId") UUID tenantId,
            @Valid @RequestBody PaymentProofRequest request) {
        if (!tenantId.equals(requireUserId())) {
            throw new UnauthorizedException("Tenant ID does not match authenticated user");
        }
        return ResponseEntity.ok(paymentProofService.submitPaymentProof(tenantId, request));
    }

    /**
     * PATCH 7: Get pending payment proofs for PM review
     */
    @GetMapping("/api/payments/proofs/pending")
    public ResponseEntity<PagedResponse<PaymentProofDto>> getPendingProofs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentProofService.getPendingProofs(requireTenant(), page, size));
    }

    /**
     * PATCH 7: Resolve a payment proof (match or reject)
     */
    @PutMapping("/api/payments/proofs/{id}/resolve")
    public ResponseEntity<PaymentProofDto> resolveProof(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PaymentProofResolveRequest request) {
        return ResponseEntity.ok(paymentProofService.resolveProof(id, requireTenant(), request));
    }

    /**
     * PATCH 7: Get proof by ID
     */
    @GetMapping("/api/payments/proofs/{id}")
    public ResponseEntity<PaymentProofDto> getProof(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentProofService.getProof(id, requireTenant()));
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