package com.urbano.monolith.auth.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.UserRole;
import com.urbano.common.enums.UserStatus;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.common.exception.ValidationException;
import com.urbano.monolith.auth.dto.StaffInviteRequest;
import com.urbano.monolith.auth.dto.StaffUserResponse;
import com.urbano.monolith.auth.dto.UpdateUserStatusRequest;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffUserService {

    private static final EnumSet<UserRole> STAFF_ROLES =
            EnumSet.of(UserRole.PM_ADMIN, UserRole.PM_STAFF);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<StaffUserResponse> listStaff(int page, int size) {
        UUID pmAccountId = requireTenant();
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<User> users = userRepository.findByPmAccountIdAndRoleIn(pmAccountId, STAFF_ROLES, pageable);
        return users.map(this::toDto);
    }

    @Transactional
    public StaffUserResponse inviteStaff(StaffInviteRequest request) {
        UUID pmAccountId = requireTenant();

        if (request.getRole() != UserRole.PM_STAFF && request.getRole() != UserRole.PM_ADMIN) {
            throw new ValidationException("Role must be PM_STAFF or PM_ADMIN");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already registered");
        }

        // Generate a random temporary password. Staff logs in and can change it
        // via /api/auth/change-password. Send invite link via SMS/email in a
        // follow-up (out of scope for this commit).
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .pmAccountId(pmAccountId)
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .phoneVerified(false)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        log.info("Staff user invited: {} role={} for PM {}",
                user.getEmail(), user.getRole(), pmAccountId);

        return toDto(user);
    }

    @Transactional
    public StaffUserResponse updateStatus(UUID userId, UpdateUserStatusRequest request) {
        UUID pmAccountId = requireTenant();

        User user = userRepository.findByIdAndPmAccountId(userId, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Guard: don't allow last PM_ADMIN to deactivate themselves.
        if (user.getRole() == UserRole.PM_ADMIN
                && request.getStatus() != UserStatus.ACTIVE
                && user.getStatus() == UserStatus.ACTIVE) {
            long remainingAdmins = userRepository.countByPmAccountIdAndRole(pmAccountId, UserRole.PM_ADMIN);
            if (remainingAdmins <= 1) {
                throw new ValidationException(
                        "Cannot deactivate the last active PM_ADMIN for this account");
            }
        }

        if (user.getId().equals(TenantContext.getUserId())
                && request.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("You cannot deactivate your own account");
        }

        user.setStatus(request.getStatus());
        user.setActive(request.getStatus() == UserStatus.ACTIVE);
        user = userRepository.save(user);

        log.info("Staff user {} status -> {}", user.getEmail(), user.getStatus());
        return toDto(user);
    }

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("PM account context required");
        }
        return pmAccountId;
    }

    private StaffUserResponse toDto(User user) {
        return StaffUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}