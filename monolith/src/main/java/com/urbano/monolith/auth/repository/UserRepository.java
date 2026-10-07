package com.urbano.monolith.auth.repository;

import com.urbano.common.enums.UserRole;
import com.urbano.monolith.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByRole(UserRole role);
    List<User> findByRole(UserRole role);

    List<User> findByPmAccountIdInAndRole(Collection<UUID> pmAccountIds, UserRole role);

    // ---------------------------------------------------------------
    // Staff management
    // ---------------------------------------------------------------
    Page<User> findByPmAccountIdAndRoleIn(UUID pmAccountId, Collection<UserRole> roles, Pageable pageable);

    long countByPmAccountIdAndRole(UUID pmAccountId, UserRole role);

    Optional<User> findByIdAndPmAccountId(UUID id, UUID pmAccountId);
}