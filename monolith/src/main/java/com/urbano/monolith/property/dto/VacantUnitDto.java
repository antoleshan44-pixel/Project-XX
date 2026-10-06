package com.urbano.monolith.property.dto;

import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
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

    // ---- Unit identity / core fields ----
    private UUID id;
    private UUID propertyId;
    private String unitNumber;
    private String label;
    private Integer floor;
    private Double squareFootage;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal rentAmount;
    private String currency;

    private String description;
    private String features;
    private List<String> photoUrls;

    private String thumbnailUrl;

    private String status;
    private boolean published;

    private PropertyType unitPropertyType;

    private TransactionType transactionType;

    private String propertyName;

    private String propertyType;

    private String address;
    private String city;
    private String state;
    private String country;
    private String location;

    private PropertyApprovalStatus propertyApprovalStatus;

    // ---- PM ownership ----
    private UUID pmAccountId;

    private String pmName;
}