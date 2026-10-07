package com.urbano.monolith.auth.controller;

import com.urbano.common.exception.UnauthorizedException;
import com.urbano.common.security.JwtClaims;
import com.urbano.monolith.auth.dto.*;
import com.urbano.monolith.auth.service.AuthService;
import com.urbano.monolith.auth.service.FirebaseTokenService;
import com.urbano.monolith.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final FirebaseTokenService firebaseTokenService;

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
     * registered user. Post-registration only.
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

    /**
     * Always returns 200 with an empty body — never reveals whether the
     * identifier is registered. If it is, an OTP is sent via SMS.
     *
     * <p>New contract: caller passes { identifier } (email OR phone).
     * The OTP is stored under otp:reset:&lt;userId&gt; server-side. The client
     * never sees or needs the userId.</p>
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            UUID userId = authService.findUserIdByIdentifier(request.getIdentifier());
            String phone = authService.findPhoneByUserId(userId);
            otpService.generateAndSendResetOtp(userId.toString(), phone);
        } catch (Exception e) {
            // Swallow — never leak account existence to the caller.
            log.info("Password forgot requested for unknown or unreachable identifier");
        }
        return ResponseEntity.ok().build();
    }

    /**
     * New contract: { identifier, code, newPassword }.
     * No userId ever travels to or from the client.
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordByIdentifier(
                request.getIdentifier(),
                request.getCode(),
                request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        JwtClaims claims = requirePrincipal();
        return ResponseEntity.ok(authService.getUserProfile(claims.userId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        JwtClaims claims = requirePrincipal();
        return ResponseEntity.ok(authService.updateProfile(
                claims.userId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        JwtClaims claims = requirePrincipal();
        authService.changePassword(
                claims.userId(),
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Mints a Firebase custom token for the authenticated user.
     * Returns 503 if Firebase is not configured on the server.
     */
    @PostMapping("/firebase-token")
    public ResponseEntity<FirebaseTokenResponse> getFirebaseToken() {
        JwtClaims claims = requirePrincipal();
        String customToken = firebaseTokenService.mintCustomToken(claims.userId());
        return ResponseEntity.ok(FirebaseTokenResponse.builder()
                .customToken(customToken)
                .expiresIn(firebaseTokenService.getCustomTokenTtlSeconds())
                .build());
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private JwtClaims requirePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof JwtClaims claims)) {
            throw new UnauthorizedException("Authentication required");
        }
        return claims;
    }
}