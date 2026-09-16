package com.urbano.monolith.property.dto;

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
public class UnitRequest {
    private UUID propertyId;
    private String unitNumber;
    private Integer floor = 1;
    private Double squareFootage;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal rentAmount;
    private String currency;
    private String description;
    private String features;
}
