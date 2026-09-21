package com.urbano.monolith.auth.service;

import com.urbano.common.enums.InviteStatus;
import com.urbano.common.enums.UserRole;
import com.urbano.common.enums.UserStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.auth.dto.AuthRequest;
import com.urbano.monolith.auth.dto.AuthResponse;
import com.urbano.monolith.auth.dto.ForgotPasswordRequest;
import com.urbano.monolith.auth.dto.RefreshTokenRequest;
import com.urbano.monolith.auth.dto.RefreshTokenResponse;
import com.urbano.monolith.auth.dto.RegisterRequest;
import com.urbano.monolith.auth.dto.ResetPasswordRequest;
import com.urbano.monolith.auth.dto.TenantActivateCodeRequest;
import com.urbano.monolith.auth.dto.TenantRegistrationRequest;
import com.urbano.monolith.auth.dto.TenantRegistrationResponse;
import com.urbano.monolith.auth.dto.UserProfileResponse;
import com.urbano.monolith.auth.entity.PmAccount;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.PmAccountRepository;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.TenantRepository;
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
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;
    private final InviteTokenService inviteTokenService;   // NEW (Commit 6b)

    // ============================================================
    // REGISTER (PM_ADMIN)
    // ============================================================
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already registered");
        }

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
                .tenantId(null)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // LOGIN
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
                .tenantId(resolveTenantId(user.getId()))
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isTokenRevoked(refreshToken)) {
            throw new UnauthorizedException("Refresh token is no longer valid");
        }

        String type = jwtService.extractType(refreshToken);
        if (!"refresh".equals(type)) {
            throw new UnauthorizedException("Not a refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        tokenBlacklistService.blacklistToken(refreshToken);

        String newAccess = jwtService.generateToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // LOGOUT
    // ============================================================
    public void logout(String token) {
        tokenBlacklistService.blacklistToken(token);
        log.info("User logged out");
    }

    // ============================================================
    // TENANT REGISTRATION (self-signup, no tenants row yet)
    // ============================================================
    @Transactional
    public TenantRegistrationResponse registerTenant(TenantRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

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

        return TenantRegistrationResponse.builder()
                .tenantId(user.getId().toString())
                .message("Tenant registered successfully. Please verify your phone.")
                .build();
    }

    // ============================================================
    // COMMIT 6b: TENANT ACTIVATION (real implementation)
    // ============================================================
    /**
     * Activates a tenant from a PENDING invite.
     *
     * <ol>
     *   <li>Find tenant by phone, invite_status = PENDING</li>
     *   <li>Verify + consume the 6-digit invite code in Redis</li>
     *   <li>Create the auth_users row with role=TENANT, phoneVerified=true</li>
     *   <li>Link tenants.user_id, set is_active=true, invite_status=ACTIVATED</li>
     *   <li>Return AuthResponse with tenantId populated (the mobile app can
     *       then hit GET /api/tenants/{tenantId}/payments directly)</li>
     * </ol>
     *
     * <p>Rejects with 409 if a user already exists for this phone or email —
     * per the design decision not to implicitly reuse accounts.</p>
     */
    @Transactional
    public AuthResponse activateTenant(TenantActivateCodeRequest request) {
        // 1. Locate the PENDING tenant by phone
        Tenant tenant = tenantRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new UnauthorizedException("Invalid invite"));

        if (tenant.getInviteStatus() != InviteStatus.PENDING) {
            throw new UnauthorizedException("Invalid invite");
        }

        // 2. Validate + consume the code (deletes the Redis key on success)
        boolean verified = inviteTokenService.verifyAndConsume(
                tenant.getId().toString(), request.getCode());
        if (!verified) {
            throw new UnauthorizedException("Invalid or expired invite code");
        }

        // 3. Reject if a user already exists for this phone/email
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("A user account already exists for this phone number");
        }
        if (userRepository.existsByEmail(tenant.getEmail())) {
            throw new ConflictException("A user account already exists for this email");
        }

        // 4. Create the auth_users row
        User user = User.builder()
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(tenant.getFirstName())
                .lastName(tenant.getLastName())
                .role(UserRole.TENANT)
                .pmAccountId(tenant.getPmAccountId())
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .phoneVerified(true)   // they just proved ownership via the SMS code
                .emailVerified(false)
                .build();
        user = userRepository.save(user);
        log.info("Tenant user activated: {} linked to tenant {}", user.getEmail(), tenant.getId());

        // 5. Link tenant → user, flip statuses
        tenant.setUserId(user.getId());
        tenant.setIsActive(true);
        tenant.setInviteStatus(InviteStatus.ACTIVATED);
        tenant.setActivatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        // 6. Return the same shape as /auth/login, with tenantId populated
        String access = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .pmAccountId(user.getPmAccountId())
                .tenantId(tenant.getId())   // <-- the whole point of 6b
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ============================================================
    // PHONE VERIFICATION
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
    // PASSWORD RESET
    // ============================================================
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenBlacklistService.invalidateAllForUser(userId);
        log.info("Password reset and all sessions invalidated for user: {}", user.getEmail());
    }

    // ============================================================
    // HELPERS
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
    // GET /auth/me
    // ============================================================
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String pmAccountName = null;
        if (user.getPmAccountId() != null) {
            pmAccountName = pmAccountRepository.findById(user.getPmAccountId())
                    .map(PmAccount::getCompanyName)
                    .orElse(null);
        }

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .pmAccountId(user.getPmAccountId())
                .pmAccountName(pmAccountName)
                .tenantId(resolveTenantId(user.getId()))
                .phoneVerified(user.isPhoneVerified())
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .build();
    }

    // ============================================================
    // INTERNAL — resolve tenants.id for a given auth user
    // ============================================================
    private UUID resolveTenantId(UUID userId) {
        if (userId == null) return null;
        return tenantRepository.findByUserId(userId)
                .map(t -> t.getId())
                .orElse(null);
    }
}