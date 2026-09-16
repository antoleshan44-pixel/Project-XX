package com.urbano.monolith.payment.dto;

import com.urbano.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationDto {
    private UUID id;
    private String referenceNumber;
    private Double amount;
    private String currency;
    private LocalDateTime paymentDate;
    private PaymentStatus status;
    private boolean isReconciled;
    private LocalDateTime reconciledAt;
}