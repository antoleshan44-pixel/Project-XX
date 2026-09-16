package com.urbano.monolith.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacantUnitDto {
    private UUID id;
    private UUID propertyId;
    private String propertyName;
    private String location;
    private String label;
    private Integer bedrooms;
    private BigDecimal rentAmount;
    private String description;
    private List<String> photoUrls;
    private UUID pmAccountId;
}