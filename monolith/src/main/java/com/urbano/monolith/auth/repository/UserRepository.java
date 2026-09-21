package com.urbano.monolith.auth.repository;

import com.urbano.monolith.auth.entity.User;
import com.urbano.common.enums.UserRole;
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
    List<User> findByRole(UserRole role);

    /**
     * Batch lookup of users by PM account + role.
     *
     * <p>Used by {@code UnitService} to resolve PM display names for a page
     * of public listings in a single query instead of N+1.</p>
     */
    List<User> findByPmAccountIdInAndRole(Collection<UUID> pmAccountIds, UserRole role);
}