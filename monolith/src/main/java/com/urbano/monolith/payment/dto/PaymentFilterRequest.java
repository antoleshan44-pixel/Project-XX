package com.urbano.monolith.payment.dto;

import com.urbano.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterRequest {
    private PaymentStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String referenceNumber;
    private String paymentMethod;
}
