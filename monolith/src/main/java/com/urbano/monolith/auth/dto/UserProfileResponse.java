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
public class UserProfileResponse {

    private UUID userId;
    private String email;
    private String phone;
    private String fullName;
    private String firstName;
    private String lastName;
    private String role;
    private UUID pmAccountId;

    private String pmAccountName;

    private UUID tenantId;

    private boolean phoneVerified;
    private boolean emailVerified;
    private boolean isActive;
}