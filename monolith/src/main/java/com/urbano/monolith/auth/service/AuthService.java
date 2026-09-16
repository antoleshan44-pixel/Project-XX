package com.urbano.monolith.auth.service;

import com.urbano.monolith.auth.dto.AuthRequest;
import com.urbano.monolith.auth.dto.AuthResponse;
import com.urbano.monolith.auth.dto.ForgotPasswordRequest;
import com.urbano.monolith.auth.dto.RefreshTokenRequest;
import com.urbano.monolith.auth.dto.RefreshTokenResponse;
import com.urbano.monolith.auth.dto.RegisterRequest;
import com.urbano.monolith.auth.dto.ResetPasswordRequest;
import com.urbano.monolith.auth.dto.TenantActivateRequest;
import com.urbano.monolith.auth.dto.TenantRegistrationRequest;
import com.urbano.monolith.auth.dto.TenantRegistrationResponse;
import com.urbano.monolith.auth.dto.UserProfileResponse;
import com.urbano.monolith.auth.entity.PmAccount;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.PmAccountRepository;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.common.enums.UserRole;
import com.urbano.common.enums.UserStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PmAccountRepository pmAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;

    // ============================================================
    // PATCH 0: FIXED - PM_ADMIN Registration with PmAccount
    // ============================================================
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already registered");
        }

        // ✅ FIX: Remove .id() - let Hibernate generate the UUID
        PmAccount pmAccount = PmAccount.builder()
                .companyName(request.getCompanyName())
                .serviceOption("SOFTWARE_ONLY")
                .isActive(true)
                .build();
        pmAccount = pmAccountRepository.save(pmAccount);
        log.info("PmAccount created: {}", pmAccount.getId());

        String[] nameParts = request.getFullName().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // ✅ FIX: Remove .id() and .createdAt() - let Hibernate handle them
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(firstName)
                .lastName(lastName)
                .role(UserRole.PM_ADMIN)
                .pmAccountId(pmAccount.getId())
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .phoneVerified(false)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("PM_ADMIN registered: {} with account {}", user.getEmail(), pmAccount.getId());

        // ✅ FIX: Comment out OTP for testing if SMS is not configured
        // otpService.generateAndSendPhoneOtp(user.getPhone());

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .pmAccountId(user.getPmAccountId())
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // LOGIN - Returns full user info with pmAccountId
    // ============================================================
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("Account is suspended");
        }

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .pmAccountId(user.getPmAccountId())
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        if (tokenBlacklistService.isTokenBlacklisted(request.getRefreshToken())) {
            throw new UnauthorizedException("Refresh token is blacklisted");
        }

        String email = jwtService.extractEmail(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newToken = jwtService.generateToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // LOGOUT - Blacklist token
    // ============================================================
    public void logout(String token) {
        tokenBlacklistService.blacklistToken(token);
        log.info("User logged out");
    }

    // ============================================================
    // TENANT REGISTRATION - Unchanged (stays as TENANT)
    // ============================================================
    @Transactional
    public TenantRegistrationResponse registerTenant(TenantRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        // ✅ FIX: Remove .id() and .createdAt() - let Hibernate handle them
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName("Tenant")
                .lastName("User")
                .role(UserRole.TENANT)
                .pmAccountId(null)
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .phoneVerified(false)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("Tenant registered: {}", user.getEmail());

        // Comment out OTP for testing if SMS is not configured
        // otpService.generateAndSendPhoneOtp(user.getPhone());

        return TenantRegistrationResponse.builder()
                .tenantId(user.getId().toString())
                .message("Tenant registered successfully. Please verify your phone.")
                .build();
    }

    // ============================================================
    // TENANT ACTIVATION - Will be rebuilt with OTP
    // ============================================================
    @Transactional
    public void activateTenant(TenantActivateRequest request) {
        // TODO: Rebuild to use Redis-OTP + SmsService mechanism
        // This is the PM-invite flow - should set phoneVerified when complete
        log.info("Tenant activated with token: {}", request.getToken());
    }

    // ============================================================
    // PATCH 1: Phone Verification
    // ============================================================
    @Transactional
    public void markPhoneVerified(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPhoneVerified(true);
        userRepository.save(user);
        log.info("Phone verified for user: {}", user.getEmail());
    }

    // ============================================================
    // PATCH 1: Password Reset
    // ============================================================
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenBlacklistService.blacklistAllUserTokens(userId);
        log.info("Password reset for user: {}", user.getEmail());
    }

    // ============================================================
    // PATCH 1: Helper Methods
    // ============================================================
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public UUID findUserIdByIdentifier(String identifier) {
        return userRepository.findByEmail(identifier)
                .map(User::getId)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .map(User::getId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                );
    }

    public String findPhoneByUserId(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ============================================================
    // PATCH 2: GET /auth/me
    // ============================================================
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .pmAccountId(user.getPmAccountId())
                .phoneVerified(user.isPhoneVerified())
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .build();
    }
}