package com.urbano.monolith.auth.dto;

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
    private String name;
    private String companyRegNumber;
    private PmAccountStatus status;
    private LocalDateTime approvedAt;
    private UUID approvedBy;
    private String rejectionReason;
    private String suspendedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
