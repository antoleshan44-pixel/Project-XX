package com.urbano.monolith.property.dto;

import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.enums.UnitStatus;
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
public class UnitDto {
    private UUID id;
    private UUID propertyId;
    private String unitNumber;
    private Integer floor;
    private Double squareFootage;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double rentAmount;
    private String currency;
    private Boolean isAvailable;
    private Boolean published;
    private UnitStatus status;
    private String description;
    private String features;
    private List<String> photoUrls;

    // ---- Commit 7 ----
    private PropertyType propertyType;
    private TransactionType transactionType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}