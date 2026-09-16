package com.urbano.monolith.maintenance.repository;

import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.monolith.maintenance.entity.MaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, UUID> {

    // ============================================================
    // SCOPED QUERIES (with PM Account)
    // ============================================================

    /**
     * Find a maintenance request by ID and PM Account ID
     * Ensures PM can only access their own requests
     */
    Optional<MaintenanceRequest> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    /**
     * Get all maintenance requests for a PM account
     */
    Page<MaintenanceRequest> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    /**
     * Get maintenance requests by PM account and status
     */
    Page<MaintenanceRequest> findByPmAccountIdAndStatus(UUID pmAccountId, MaintenanceStatus status, Pageable pageable);

    /**
     * Get maintenance requests by unit and PM account
     */
    Page<MaintenanceRequest> findByUnitIdAndPmAccountId(UUID unitId, UUID pmAccountId, Pageable pageable);

    /**
     * Get maintenance requests by property and PM account
     */
    Page<MaintenanceRequest> findByPropertyIdAndPmAccountId(UUID propertyId, UUID pmAccountId, Pageable pageable);

    // ============================================================
    // ✅ TENANT QUERIES (for TenantMaintenanceController)
    // ============================================================

    /**
     * Find all maintenance requests for a specific tenant
     * Used by TenantMaintenanceController - no PM scoping needed
     */
    Page<MaintenanceRequest> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find a specific maintenance request by ID and tenant ID
     * Used by TenantMaintenanceController - ensures tenant only sees their own requests
     */
    Optional<MaintenanceRequest> findByIdAndTenantId(UUID id, UUID tenantId);

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