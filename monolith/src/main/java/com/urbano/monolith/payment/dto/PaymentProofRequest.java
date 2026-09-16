package com.urbano.monolith.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    private String mpesaReceiptNumber;  // Optional

    @NotBlank(message = "Screenshot URL is required")
    private String screenshotUrl;

    @NotNull(message = "Amount claimed is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amountClaimed;
}