package com.urbano.monolith.payment.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.PaymentSummaryDto;
import com.urbano.monolith.payment.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    /**
     * PATCH 8: Super admin payment summary across all accounts
     */
    @GetMapping("/summary")
    public ResponseEntity<PaymentSummaryDto> getPaymentSummary(
            @RequestHeader("adminAccess") boolean adminAccess,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // ✅ adminAccess header must be true
        if (!adminAccess) {
            throw new SecurityException("Admin access required");
        }

        return ResponseEntity.ok(adminPaymentService.getPaymentSummary(startDate, endDate));
    }

    /**
     * PATCH 8: Super admin payment list across all accounts
     */
    @GetMapping
    public ResponseEntity<PagedResponse<PaymentDto>> getAllPayments(
            @RequestHeader("adminAccess") boolean adminAccess,
            @RequestParam(value = "pmAccountId", required = false) UUID pmAccountId,
            @RequestParam(value = "status", required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {

        // ✅ adminAccess header must be true
        if (!adminAccess) {
            throw new SecurityException("Admin access required");
        }

        return ResponseEntity.ok(adminPaymentService.getPayments(pmAccountId, status, startDate, endDate, pageable));
    }
}