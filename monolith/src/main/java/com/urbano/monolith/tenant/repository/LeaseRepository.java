package com.urbano.monolith.tenant.repository;

import com.urbano.common.enums.LeaseStatus;
import com.urbano.monolith.tenant.entity.Lease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    // ============================================================
    // SCOPED QUERIES (with PM Account)
    // ============================================================
    @EntityGraph(attributePaths = {"tenant"})
    Optional<Lease> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<Lease> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Lease> findByPmAccountIdAndIsActiveTrue(UUID pmAccountId, Pageable pageable);

    @EntityGraph(attributePaths = {"tenant"})
    Page<Lease> findByTenantIdAndPmAccountId(UUID tenantId, UUID pmAccountId, Pageable pageable);

    Page<Lease> findByUnitIdAndPmAccountId(UUID unitId, UUID pmAccountId, Pageable pageable);

    // ============================================================
    // ACTIVE LEASE QUERIES
    // ============================================================
    Optional<Lease> findByUnitIdAndIsActiveTrue(UUID unitId);

    Optional<Lease> findByTenantIdAndIsActiveTrue(UUID tenantId);

    boolean existsByTenantIdAndIsActiveTrue(UUID tenantId);

    boolean existsByUnitIdAndIsActiveTrue(UUID unitId);

    boolean existsByIdAndIsActiveTrue(UUID id);

    // ============================================================
    // LEGACY QUERIES (deprecated)
    // ============================================================
    @Deprecated
    Page<Lease> findByTenantId(UUID tenantId, Pageable pageable);

    @Deprecated
    Page<Lease> findByPropertyId(UUID propertyId, Pageable pageable);

    @Deprecated
    Page<Lease> findByStatus(LeaseStatus status, Pageable pageable);

    @Deprecated
    Optional<Lease> findFirstByUnitIdAndStatus(UUID unitId, LeaseStatus status);

    @Deprecated
    Optional<Lease> findFirstByTenantIdAndStatus(UUID tenantId, LeaseStatus status);

    // ============================================================
    // OVERRIDE findById to eagerly fetch tenant
    // ============================================================
    @Override
    @EntityGraph(attributePaths = {"tenant"})
    Optional<Lease> findById(UUID id);
}