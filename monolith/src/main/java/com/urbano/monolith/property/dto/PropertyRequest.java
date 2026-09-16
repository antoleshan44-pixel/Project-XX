package com.urbano.monolith.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRequest {
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String type;
    private Integer totalUnits;
    private UUID ownerId;
    private String ownerName;
    private String ownerEmail;
    private UUID pmAccountId;
    private String amenities;
    private Double latitude;
    private Double longitude;
}