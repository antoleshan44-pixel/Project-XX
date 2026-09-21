package com.urbano.monolith.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantActivateCodeRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Invite code is required")
    @Size(min = 6, max = 6, message = "Invite code must be 6 digits")
    private String code;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}