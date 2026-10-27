package com.urbano.monolith.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response body for all authentication endpoints (login, register, activate).
 *
 * <p><strong>Null-field policy:</strong> fields whose value is null are
 * OMITTED from the serialized JSON (via {@link JsonInclude.Include#NON_NULL}).
 * This is intentional — the mobile client expects {@code tenantId},
 * {@code pmAccountId}, and {@code firebaseCustomToken} to be either absent or
 * a string, never the literal {@code null}. See issue: strict JSON parser
 * rejecting {@code "tenantId":null} on PM_ADMIN logins.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String email;
    private String phone;
    private String fullName;
    private String role;
    private UUID pmAccountId;

    private UUID tenantId;

    private String firebaseCustomToken;

    private String tokenType;
    private Integer expiresIn;
}