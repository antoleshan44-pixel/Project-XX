package com.urbano.monolith.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponse {
    private UUID id;
    private UUID propertyId;
    private String unitNumber;
    private String status;
    private Double rentAmount;
    private String currency;
}