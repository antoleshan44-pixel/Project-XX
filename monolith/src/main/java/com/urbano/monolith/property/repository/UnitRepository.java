package com.urbano.monolith.property.repository;

import com.urbano.monolith.property.entity.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    // ============================================================
    // Tenant-scoped queries
    // ============================================================

    @Query("SELECT u FROM Unit u WHERE u.property.pmAccountId = :pmAccountId AND u.deletedAt IS NULL")
    Page<Unit> findByTenant(@Param("pmAccountId") UUID pmAccountId, Pageable pageable);

    @Query("SELECT u FROM Unit u WHERE u.id = :id AND u.property.pmAccountId = :pmAccountId AND u.deletedAt IS NULL")
    Optional<Unit> findByIdAndTenant(@Param("id") UUID id, @Param("pmAccountId") UUID pmAccountId);

    @Query("SELECT u FROM Unit u WHERE u.propertyId = :propertyId AND u.property.pmAccountId = :pmAccountId AND u.deletedAt IS NULL")
    Page<Unit> findByPropertyIdAndTenant(@Param("propertyId") UUID propertyId,
                                         @Param("pmAccountId") UUID pmAccountId,
                                         Pageable pageable);

    @Query("SELECT COUNT(u) > 0 FROM Unit u WHERE u.id = :unitId AND u.property.pmAccountId = :pmAccountId")
    boolean existsByIdAndTenant(@Param("unitId") UUID unitId, @Param("pmAccountId") UUID pmAccountId);

    // ============================================================
    // Public queries (no tenant context — used for public listings)
    // ============================================================

    List<Unit> findByIsAvailableTrueAndPublishedTrue();

    Page<Unit> findByIsAvailableTrue(Pageable pageable);

    // ============================================================
    // Internal — used by LeaseService/MaintenanceService for cross-module ops
    // ============================================================

    Page<Unit> findByPropertyId(UUID propertyId, Pageable pageable);
}