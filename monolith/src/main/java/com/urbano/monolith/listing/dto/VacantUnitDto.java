package com.urbano.monolith.listing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacantUnitDto {
    private UUID id;
    private UUID propertyId;
    private String propertyName;
    private String unitNumber;
    private Double rentAmount;
    private String currency;
    private Integer bedrooms;
    private Integer bathrooms;
    private String propertyType;
    private Double squareFootage;
    private String address;
    private String city;
    private String state;
    private String description;
    private String thumbnailUrl;
    private String status;
    private boolean published;
}