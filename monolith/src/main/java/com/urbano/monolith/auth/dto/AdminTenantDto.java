package com.urbano.monolith.auth.dto;

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
public class AdminTenantDto {

    private UUID id;
    private UUID pmAccountId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private UUID unitId;
    private Boolean isActive;
    private Double creditBalance;
    private String inviteStatus;
    private LocalDateTime invitedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}