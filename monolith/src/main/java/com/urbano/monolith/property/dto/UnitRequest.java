package com.urbano.monolith.property.dto;

import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
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

    @Builder.Default
    private Integer floor = 1;

    private Double squareFootage;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal rentAmount;
    private String currency;
    private String description;
    private String features;

    /** Fine-grained unit type. Defaults to APARTMENT on create if omitted. */
    private PropertyType propertyType;

    /** FOR_SALE or FOR_RENT. Defaults to FOR_RENT on create if omitted. */
    private TransactionType transactionType;
}