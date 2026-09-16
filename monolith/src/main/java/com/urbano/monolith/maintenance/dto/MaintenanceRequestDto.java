package com.urbano.monolith.maintenance.dto;

import com.urbano.common.enums.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestDto {
    private UUID id;
    private UUID pmAccountId;
    private UUID propertyId;
    private String propertyName;
    private UUID unitId;
    private String unitNumber;
    private UUID tenantId;
    private String tenantName;
    private String title;
    private String description;
    private String priority;
    private MaintenanceStatus status;
    private UUID assignedTo;
    private String assignedToName;
    private LocalDateTime scheduledDate;
    private LocalDateTime completedDate;
    private LocalDateTime resolvedAt;
    private String notes;
    private String photoUrl;
    private List<String> photoUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}