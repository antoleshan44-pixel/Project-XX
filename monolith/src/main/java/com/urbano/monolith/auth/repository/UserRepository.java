package com.urbano.monolith.auth.repository;

import com.urbano.monolith.auth.entity.User;
import com.urbano.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);  // ✅ Added
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);  // ✅ Added
    List<User> findByRole(UserRole role);
}