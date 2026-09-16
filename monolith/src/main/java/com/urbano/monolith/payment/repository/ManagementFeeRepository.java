package com.urbano.monolith.payment.repository;

import com.urbano.monolith.payment.entity.ManagementFee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManagementFeeRepository extends JpaRepository<ManagementFee, UUID> {

    // ============================================================
    // SCOPED QUERIES
    // ============================================================
    Page<ManagementFee> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    List<ManagementFee> findByPmAccountId(UUID pmAccountId);

    Optional<ManagementFee> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    // ============================================================
    // PROPERTY QUERIES
    // ============================================================
    List<ManagementFee> findByPropertyId(UUID propertyId);

    List<ManagementFee> findByPropertyIdAndIsActiveTrue(UUID propertyId);

    // ============================================================
    // UNIT QUERIES
    // ============================================================
    Optional<ManagementFee> findByUnitId(UUID unitId);

    Optional<ManagementFee> findByUnitIdAndIsActiveTrue(UUID unitId);

    List<ManagementFee> findByUnitIdAndIsActiveTrueOrderByCreatedAtDesc(UUID unitId);

    // ============================================================
    // PERIOD QUERIES
    // ============================================================
    List<ManagementFee> findByPmAccountIdAndPeriod(UUID pmAccountId, YearMonth period);

    List<ManagementFee> findByPmAccountIdAndStatus(UUID pmAccountId, String status);

    // ============================================================
    // BULK QUERIES
    // ============================================================
    List<ManagementFee> findByStatus(String status);
}