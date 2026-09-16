package com.urbano.monolith.payment.dto;

import com.urbano.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID unitId;
    private String unitLabel;
    private UUID tenantId;
    private String tenantName;
    private BigDecimal amount;
    private BigDecimal amountExpected;
    private BigDecimal amountDifference;
    private String mpesaReceiptNumber;
    private String customerName;
    private LocalDateTime transactionDate;
    private PaymentStatus status;
    private LocalDateTime reconciledAt;
}