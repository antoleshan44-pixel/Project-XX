package com.urbano.monolith.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private UUID id;
    private UUID pmAccountId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private UUID unitId;
    private String unitNumber;
    private String idNumber;
    private String emergencyContact;
    private String emergencyPhone;
    private Boolean isActive;
    private Double creditBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}