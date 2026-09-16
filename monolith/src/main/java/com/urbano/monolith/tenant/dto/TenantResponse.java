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
public class TenantResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private UUID pmAccountId;
    private UUID unitId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}