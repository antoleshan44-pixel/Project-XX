package com.urbano.monolith.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInviteCodeDto {

    private UUID tenantId;
    private String tenantFullName;
    private String tenantPhone;
    private String tenantEmail;
    private String inviteStatus;   // PENDING / ACTIVATED / MANUAL
    private String code;           // null when status != PENDING or code expired
    private Long ttlSeconds;       // remaining seconds; null when code is gone
}