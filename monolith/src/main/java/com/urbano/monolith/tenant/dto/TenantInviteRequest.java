package com.urbano.monolith.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Commit 6b: payload for POST /api/tenants/invite.
 *
 * <p>pmAccountId is NOT accepted from the client — it always comes from
 * the JWT via {@code TenantContext.getPmAccountId()}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInviteRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 digits")
    private String phone;

    /**
     * Optional — the PM can attach the invite to a specific unit up front.
     * The unit must belong to the same pmAccountId (validated in service layer
     * is a follow-up; for now the assignment is honored as-is).
     */
    private UUID unitId;
}