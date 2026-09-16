package com.urbano.monolith.listing.dto;

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
public class ListingDto {
    private UUID id;
    private UUID pmAccountId;
    private UUID propertyId;
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
    private String status;
    private boolean published;
    private List<String> photoUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}