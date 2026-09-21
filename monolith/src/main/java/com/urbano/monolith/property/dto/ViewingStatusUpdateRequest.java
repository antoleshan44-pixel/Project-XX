package com.urbano.monolith.property.dto;

import com.urbano.common.enums.ViewingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewingStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ViewingStatus status;

    /** Optional note. Overwrites any existing notes if provided. */
    private String notes;
}