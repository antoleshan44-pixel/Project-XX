package com.urbano.monolith.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofResolveRequest {

    private UUID matchedPaymentId;  // If MATCHED

    @NotNull(message = "Resolution status is required")
    private String resolution;  // "MATCHED" or "REJECTED"

    private String notes;
}