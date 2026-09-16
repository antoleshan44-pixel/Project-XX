package com.urbano.monolith.tenant.dto;

import com.urbano.common.enums.LeaseStatus;
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
public class LeaseResponse {
    private UUID id;
    private UUID tenantId;
    private UUID unitId;
    private UUID propertyId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal rentAmount;
    private String currency;
    private BigDecimal securityDeposit;
    private String paymentFrequency;
    private String terms;
    private LeaseStatus status;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}