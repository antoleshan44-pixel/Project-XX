package com.urbano.monolith.maintenance.repository;

import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.monolith.maintenance.entity.MaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, UUID> {

    // ============================================================
    // SCOPED QUERIES (with PM Account)
    // ============================================================
    Optional<MaintenanceRequest> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<MaintenanceRequest> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<MaintenanceRequest> findByPmAccountIdAndStatus(
            UUID pmAccountId, MaintenanceStatus status, Pageable pageable);

    Page<MaintenanceRequest> findByUnitIdAndPmAccountId(
            UUID unitId, UUID pmAccountId, Pageable pageable);

    Page<MaintenanceRequest> findByPropertyIdAndPmAccountId(
            UUID propertyId, UUID pmAccountId, Pageable pageable);

    // ============================================================
    // TENANT QUERIES (for TenantMaintenanceController)
    // ============================================================
    Page<MaintenanceRequest> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<MaintenanceRequest> findByIdAndTenantId(UUID id, UUID tenantId);

    // ============================================================
    // DASHBOARD — single-query open-maintenance count
    // ============================================================
    long countByPmAccountIdAndStatusIn(UUID pmAccountId, Collection<MaintenanceStatus> statuses);

    // ============================================================
    // LEGACY QUERIES (deprecated, will be removed)
    // ============================================================
    @Deprecated
    Page<MaintenanceRequest> findByPropertyId(UUID propertyId, Pageable pageable);

    @Deprecated
    Page<MaintenanceRequest> findByUnitId(UUID unitId, Pageable pageable);

    @Deprecated
    Page<MaintenanceRequest> findByStatus(MaintenanceStatus status, Pageable pageable);
}