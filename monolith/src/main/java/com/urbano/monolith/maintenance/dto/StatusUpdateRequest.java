package com.urbano.monolith.maintenance.dto;

import com.urbano.common.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    @NotNull(message = "Status is required")
    private MaintenanceStatus status;

    private String notes;

    @Builder.Default
    private boolean notifyTenant = true;  // ✅ Added for notifications
}