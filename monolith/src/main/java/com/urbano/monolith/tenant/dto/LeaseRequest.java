package com.urbano.monolith.tenant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseRequest {

    @NotNull(message = "PM Account ID is required")
    private UUID pmAccountId;  // ✅ Added

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Rent amount is required")
    @DecimalMin(value = "0.01", message = "Rent amount must be greater than 0")
    private Double rentAmount;

    private String currency;
    private Double securityDeposit;
    private String paymentFrequency;
    private String terms;
}