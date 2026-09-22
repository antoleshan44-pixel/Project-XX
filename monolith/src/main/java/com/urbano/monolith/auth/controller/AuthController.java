package com.urbano.monolith.auth.controller;

import com.urbano.common.exception.UnauthorizedException;
import com.urbano.common.security.JwtClaims;
import com.urbano.monolith.auth.dto.*;
import com.urbano.monolith.auth.service.AuthService;
import com.urbano.monolith.auth.service.FirebaseTokenService;
import com.urbano.monolith.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final FirebaseTokenService firebaseTokenService;   // NEW (Commit 4)

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

    /**
     * Sends a 6-digit OTP to a phone number that already belongs to a
     * registered user.
     *
     * <p><strong>Post-registration only.</strong> The {@code auth_users} row
     * must already exist — created either by {@code POST /api/auth/register}
     * (PM_ADMIN signup) or by {@code POST /api/auth/tenant/register}
     * (TENANT signup). This endpoint does <em>not</em> create accounts and
     * does <em>not</em> accept anonymous phone numbers.</p>
     */
    @PostMapping("/register/verify-phone")
    public ResponseEntity<PhoneVerifyResponse> sendPhoneOtp(
            @Valid @RequestBody PhoneVerifyRequest request) {
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
            throw new UnauthorizedException("Invalid or expired reset code");
        }
        authService.resetPassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof JwtClaims claims) {
            return ResponseEntity.ok(authService.getUserProfile(claims.userId()));
        }

        return ResponseEntity.status(401).build();
    }

    /**
     * Commit 4: mints a Firebase custom token for the authenticated user.
     *
     * <p>The client calls {@code signInWithCustomToken(customToken)} to
     * establish a Firebase Auth session, which then populates
     * {@code request.auth.uid} in Firestore. Enables real per-user security
     * rules instead of the test-mode rules that expire 2026-10-17.</p>
     *
     * <p>Requires a valid JWT (this endpoint is authenticated via
     * {@code .anyRequest().authenticated()} in SecurityConfig). Returns
     * 503 if Firebase is not configured on the server.</p>
     */
    @PostMapping("/firebase-token")
    public ResponseEntity<FirebaseTokenResponse> getFirebaseToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof JwtClaims claims)) {
            return ResponseEntity.status(401).build();
        }

        String customToken = firebaseTokenService.mintCustomToken(claims.userId());
        return ResponseEntity.ok(FirebaseTokenResponse.builder()
                .customToken(customToken)
                .expiresIn(firebaseTokenService.getCustomTokenTtlSeconds())
                .build());
    }
}