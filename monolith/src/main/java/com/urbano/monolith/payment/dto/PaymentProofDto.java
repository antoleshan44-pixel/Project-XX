package com.urbano.monolith.payment.dto;

import com.urbano.monolith.payment.entity.PaymentProof;
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
public class PaymentProofDto {
    private UUID id;
    private UUID pmAccountId;
    private UUID tenantId;
    private UUID unitId;
    private UUID paymentId;
    private String mpesaReceiptNumber;
    private String screenshotUrl;
    private BigDecimal amountClaimed;
    private PaymentProof.PaymentProofStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private String resolutionNotes;
    private LocalDateTime createdAt;
}