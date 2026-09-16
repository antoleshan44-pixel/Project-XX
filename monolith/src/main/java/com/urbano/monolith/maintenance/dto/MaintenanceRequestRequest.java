package com.urbano.monolith.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
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
public class MaintenanceRequestRequest {

    @NotNull(message = "PM Account ID is required")
    private UUID pmAccountId;  // ✅ Added

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String priority; // LOW, MEDIUM, HIGH, URGENT
}