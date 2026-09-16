package com.urbano.monolith.auth.controller;

import com.urbano.monolith.auth.dto.TenantActivateRequest;
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

    @PostMapping("/register")
    public ResponseEntity<TenantRegistrationResponse> register(@Valid @RequestBody TenantRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerTenant(request));
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> activateTenant(@Valid @RequestBody TenantActivateRequest request) {
        authService.activateTenant(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<Void> verifyTenant(@PathVariable("token") String token) {
        // In a real implementation, this would verify the tenant
        return ResponseEntity.ok().build();
    }
}
