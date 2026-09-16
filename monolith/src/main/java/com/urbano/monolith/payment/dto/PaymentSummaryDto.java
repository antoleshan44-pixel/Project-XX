package com.urbano.monolith.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDto {
    private long totalPayments;
    private long reconciledCount;
    private long partialCount;
    private long overpaidCount;
    private long unmatchedCount;
    private long pendingReviewCount;
    private BigDecimal totalAmount;
    private BigDecimal reconciledAmount;
    private BigDecimal partialAmount;
    private BigDecimal overpaidAmount;
    private BigDecimal unmatchedAmount;
}