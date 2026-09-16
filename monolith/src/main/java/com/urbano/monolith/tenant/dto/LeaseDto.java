package com.urbano.monolith.tenant.dto;

import com.urbano.common.enums.LeaseStatus;
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
public class LeaseDto {
    private UUID id;
    private UUID pmAccountId;
    private UUID tenantId;
    private String tenantName;
    private UUID unitId;
    private String unitNumber;
    private UUID propertyId;
    private String propertyName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double rentAmount;
    private String currency;
    private Double securityDeposit;
    private String paymentFrequency;
    private LeaseStatus status;
    private boolean isActive;
    private LocalDateTime signedAt;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private String terms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}