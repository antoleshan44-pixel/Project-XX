package com.urbano.monolith.seeds;

import com.urbano.common.enums.UserRole;
import com.urbano.common.enums.UserStatus;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first SUPER_ADMIN on application startup if none exists.
 *
 * <p>Rationale: BCrypt hashes cannot be produced inside a Flyway SQL migration
 * without an external tool. Doing it here means Spring's own
 * {@link PasswordEncoder} produces the hash, so the password is guaranteed
 * to match what the operator typed.</p>
 *
 * <p>Idempotent: skips if any SUPER_ADMIN already exists in auth_users.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.superadmin.email:superadmin@urbano.homes}")
    private String email;

    @Value("${seed.superadmin.password:Billion$2030}")
    private String password;

    @Value("${seed.superadmin.first-name:Urbano}")
    private String firstName;

    @Value("${seed.superadmin.last-name:Admin}")
    private String lastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.info("[seed] SUPER_ADMIN already exists — skipping bootstrap");
            return;
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("[seed] auth_users.email='{}' already exists but is not SUPER_ADMIN — "
                    + "bootstrap will NOT overwrite it. Create the super-admin manually.", email);
            return;
        }

        User superAdmin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phone(null)
                .role(UserRole.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .pmAccountId(null)
                .phoneVerified(true)
                .emailVerified(true)
                .isActive(true)
                .build();

        userRepository.save(superAdmin);
        log.warn("[seed] Created initial SUPER_ADMIN '{}'. "
                + "CHANGE THIS PASSWORD IMMEDIATELY after first login.", email);
    }
}