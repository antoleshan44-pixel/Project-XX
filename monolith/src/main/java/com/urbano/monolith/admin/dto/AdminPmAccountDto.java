package com.urbano.monolith.admin.dto;

import com.urbano.common.enums.PmAccountStatus;
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
public class AdminPmAccountDto {
    private UUID id;
    private String companyName;
    private String serviceOption;
    private Boolean isActive;
    private PmAccountStatus approvalStatus;
    private String suspendedReason;
    private LocalDateTime suspendedAt;
    private UUID suspendedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}