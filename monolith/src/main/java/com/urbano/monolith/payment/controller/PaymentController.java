package com.urbano.monolith.payment.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.PaymentRequest;
import com.urbano.monolith.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PaymentDto>> getPayments(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(PaymentStatus.PENDING, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PagedResponse<PaymentDto>> getPaymentsByTenant(
            @PathVariable("tenantId") UUID tenantId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByTenant(tenantId, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<PagedResponse<PaymentDto>> getPaymentsByProperty(
            @PathVariable("propertyId") UUID propertyId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByProperty(propertyId, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<PaymentDto>> getPaymentsByStatus(
            @PathVariable("status") PaymentStatus status, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentDto> updatePaymentStatus(
            @PathVariable("id") UUID id, @RequestParam("status") PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, status));
    }

    @PostMapping("/{id}/reconcile")
    public ResponseEntity<PaymentDto> reconcilePayment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.reconcilePayment(id));
    }

    @PostMapping("/{id}/unreconcile")
    public ResponseEntity<PaymentDto> unreconcilePayment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.unreconcilePayment(id));
    }

    @GetMapping("/unreconciled")
    public ResponseEntity<?> getUnreconciledPayments() {
        return ResponseEntity.ok(paymentService.getUnreconciledPayments());
    }
}
