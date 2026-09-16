package com.urbano.monolith.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementFeeResponse {
    private UUID id;
    private UUID propertyId;
    private String propertyName;
    private YearMonth period;
    private BigDecimal amount;
    private String status;
    private LocalDateTime invoicedAt;
    private LocalDateTime paidAt;
    private String description;
}