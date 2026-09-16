package com.urbano.common.security;

import com.urbano.common.enums.UserRole;

import java.util.UUID;

public record JwtClaims(
        UUID userId,
        UserRole role,
        UUID pmAccountId,
        String email,
        String jti
) {
    public boolean isAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    public boolean isPmAdmin() {
        return role == UserRole.PM_ADMIN;
    }

    public boolean isPmStaff() {
        return role == UserRole.PM_STAFF;
    }

    public boolean isTenant() {
        return role == UserRole.TENANT;
    }

    public boolean hasPmAccount() {
        return pmAccountId != null && (role == UserRole.PM_ADMIN || role == UserRole.PM_STAFF);
    }
}