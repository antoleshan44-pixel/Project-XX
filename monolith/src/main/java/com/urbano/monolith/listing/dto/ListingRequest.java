package com.urbano.monolith.listing.dto;

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
public class ListingRequest {

    @NotNull(message = "PM Account ID is required")
    private UUID pmAccountId;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    private String title;
    private String description;
    private Double price;
    private String currency;
    private Integer bedrooms;
    private Integer bathrooms;
    private String propertyType;
    private Double squareFootage;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Double latitude;
    private Double longitude;
}