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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<PaymentDto>> getPayments(
            @RequestParam(value = "status", required = false) PaymentStatus status,
            Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(paymentService.getPaymentsByStatus(status, pageable.getPageNumber(), pageable.getPageSize()));
        }
        return ResponseEntity.ok(paymentService.getAllPayments(pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
    public ResponseEntity<PagedResponse<PaymentDto>> getPaymentsByTenant(
            @PathVariable("tenantId") UUID tenantId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByTenant(tenantId, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<PaymentDto>> getPaymentsByProperty(
            @PathVariable("propertyId") UUID propertyId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByProperty(propertyId, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<PaymentDto>> getPaymentsByStatus(
            @PathVariable("status") PaymentStatus status, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN', 'TENANT')")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> updatePaymentStatus(
            @PathVariable("id") UUID id, @RequestParam("status") PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, status));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> reconcilePayment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.reconcilePayment(id));
    }

    @PostMapping("/{id}/unreconcile")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentDto> unreconcilePayment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.unreconcilePayment(id));
    }

    @GetMapping("/unreconciled")
    @PreAuthorize("hasAnyRole('PM_ADMIN', 'PM_STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<?> getUnreconciledPayments() {
        return ResponseEntity.ok(paymentService.getUnreconciledPayments());
    }
}
