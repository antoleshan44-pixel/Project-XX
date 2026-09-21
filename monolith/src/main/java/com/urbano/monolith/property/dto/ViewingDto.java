package com.urbano.monolith.property.dto;

import com.urbano.common.enums.ViewingStatus;
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
public class ViewingDto {
    private UUID id;
    private UUID unitId;
    private UUID propertyId;
    private UUID pmAccountId;

    private String requestedByName;
    private String requestedByPhone;
    private UUID requestedByUserId;

    private LocalDateTime scheduledAt;
    private ViewingStatus status;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}