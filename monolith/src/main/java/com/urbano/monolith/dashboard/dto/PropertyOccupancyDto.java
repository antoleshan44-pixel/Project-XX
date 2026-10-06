package com.urbano.monolith.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyOccupancyDto {
    private UUID propertyId;
    private String name;
    private long occupiedUnits;
    private long totalUnits;
}
