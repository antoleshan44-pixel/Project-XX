package com.urbano.monolith.property.dto;

import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponse {
    private UUID id;
    private UUID propertyId;
    private String label;
    private Integer bedrooms;
    private BigDecimal rentAmount;
    private String status;
    private String description;
    private List<String> photoUrls;
    private boolean published;

    // ---- Commit 7 ----
    private PropertyType propertyType;
    private TransactionType transactionType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}