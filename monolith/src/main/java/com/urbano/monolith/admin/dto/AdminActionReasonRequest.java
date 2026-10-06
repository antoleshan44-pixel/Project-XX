package com.urbano.monolith.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionReasonRequest {

    @NotBlank(message = "Reason is required for administrative actions")
    @Size(min = 4, max = 512, message = "Reason must be 4-512 characters")
    private String reason;
}