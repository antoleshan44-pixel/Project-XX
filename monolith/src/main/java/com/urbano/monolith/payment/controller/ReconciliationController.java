package com.urbano.monolith.payment.controller;

import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.ReconciliationDto;
import com.urbano.monolith.payment.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/{paymentId}/match")
    public ResponseEntity<PaymentDto> matchPayment(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(reconciliationService.matchPayment(paymentId));
    }

    @PostMapping("/{paymentId}/unmatch")
    public ResponseEntity<PaymentDto> unmatchPayment(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(reconciliationService.unmatchPayment(paymentId));
    }

    @GetMapping("/unreconciled")
    public ResponseEntity<List<ReconciliationDto>> getUnreconciledPayments() {
        return ResponseEntity.ok(reconciliationService.getUnreconciledPayments());
    }

    @GetMapping("/reconciled")
    public ResponseEntity<List<ReconciliationDto>> getReconciledPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(reconciliationService.getReconciledPayments(startDate, endDate));
    }
}
