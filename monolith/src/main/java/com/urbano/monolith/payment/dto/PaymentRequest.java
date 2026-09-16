package com.urbano.monolith.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class PaymentRequest {

    @NotNull(message = "PM Account ID is required")
    private UUID pmAccountId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Lease ID is required")
    private UUID leaseId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private BigDecimal amountExpected;

    private String currency;

    private String paymentMethod;

    private String referenceNumber;

    private LocalDateTime dueDate;

    private String description;

    private String customerName;
}