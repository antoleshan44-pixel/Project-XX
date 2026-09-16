package com.urbano.monolith.auth.controller;

import com.urbano.monolith.auth.dto.*;
import com.urbano.monolith.auth.service.AuthService;
import com.urbano.monolith.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    // ============================================================
    // EXISTING ENDPOINTS
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // PATCH 1: OTP for phone verification
    // ============================================================
    @PostMapping("/register/verify-phone")
    public ResponseEntity<PhoneVerifyResponse> sendPhoneOtp(
            @Valid @RequestBody PhoneVerifyRequest request) {
        // Check if user exists with this phone
        if (!authService.existsByPhone(request.getPhone())) {
            return ResponseEntity.badRequest().body(
                    PhoneVerifyResponse.builder()
                            .verified(false)
                            .message("Phone number not registered")
                            .build()
            );
        }
        otpService.generateAndSendPhoneOtp(request.getPhone());
        return ResponseEntity.ok(
                PhoneVerifyResponse.builder()
                        .verified(false)
                        .message("OTP sent to your phone")
                        .build()
        );
    }

    @PostMapping("/register/confirm-phone")
    public ResponseEntity<PhoneVerifyResponse> confirmPhone(
            @Valid @RequestBody PhoneVerifyRequest request) {
        boolean verified = otpService.verifyPhoneOtp(request.getPhone(), request.getCode());
        if (verified) {
            authService.markPhoneVerified(request.getPhone());
            return ResponseEntity.ok(
                    PhoneVerifyResponse.builder()
                            .verified(true)
                            .message("Phone verified successfully")
                            .build()
            );
        }
        return ResponseEntity.badRequest().body(
                PhoneVerifyResponse.builder()
                        .verified(false)
                        .message("Invalid or expired OTP")
                        .build()
        );
    }

    // ============================================================
    // PATCH 1: Password Reset
    // ============================================================
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        UUID userId = authService.findUserIdByIdentifier(request.getIdentifier());
        String phone = authService.findPhoneByUserId(userId);
        otpService.generateAndSendResetOtp(userId.toString(), phone);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean verified = otpService.verifyResetOtp(
                request.getUserId().toString(),
                request.getCode()
        );
        if (!verified) {
            throw new RuntimeException("Invalid or expired reset code");
        }
        authService.resetPassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // PATCH 2: GET /auth/me
    // ============================================================
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(authService.getUserProfile(userId));
    }
}