package com.urbano.monolith.tenant.repository;

import com.urbano.monolith.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // ============================================================
    // SCOPED QUERIES (with PM Account)
    // ============================================================
    Optional<Tenant> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<Tenant> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Tenant> findByPmAccountIdAndIsActiveTrue(UUID pmAccountId, Pageable pageable);

    Page<Tenant> findByUnitIdAndPmAccountId(UUID unitId, UUID pmAccountId, Pageable pageable);

    // ============================================================
    // BASIC QUERIES
    // ============================================================
    Optional<Tenant> findByEmail(String email);

    Optional<Tenant> findByPhone(String phone);

    Optional<Tenant> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByUserId(UUID userId);

    // ============================================================
    // LEGACY QUERIES (deprecated)
    // ============================================================
    @Deprecated
    Page<Tenant> findByDeletedAtIsNull(Pageable pageable);
}