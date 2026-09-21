package com.urbano.monolith.auth.controller;

import com.urbano.monolith.auth.dto.AuthResponse;
import com.urbano.monolith.auth.dto.TenantActivateCodeRequest;
import com.urbano.monolith.auth.dto.TenantRegistrationRequest;
import com.urbano.monolith.auth.dto.TenantRegistrationResponse;
import com.urbano.monolith.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/tenant")
@RequiredArgsConstructor
public class TenantAuthController {

    private final AuthService authService;

    /**
     * Self-serve registration without a PM invite — creates a bare TENANT user
     * with no tenant row. The invite flow (POST /api/tenants/invite →
     * POST /api/auth/tenant/activate) is the primary path.
     */
    @PostMapping("/register")
    public ResponseEntity<TenantRegistrationResponse> register(
            @Valid @RequestBody TenantRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerTenant(request));
    }

    /**
     * Commit 6b: activates a PENDING invite with a 6-digit code + new password.
     * Returns the same AuthResponse shape as /api/auth/login, with tenantId
     * populated for the newly-linked TENANT user.
     */
    @PostMapping("/activate")
    public ResponseEntity<AuthResponse> activateTenant(
            @Valid @RequestBody TenantActivateCodeRequest request) {
        return ResponseEntity.ok(authService.activateTenant(request));
    }

    /**
     * Legacy verification endpoint. Kept for API compat but unused by the
     * mobile client. Consider removing once mobile confirms no callers.
     */
    @GetMapping("/verify/{token}")
    public ResponseEntity<Void> verifyTenant(@PathVariable("token") String token) {
        return ResponseEntity.ok().build();
    }
}