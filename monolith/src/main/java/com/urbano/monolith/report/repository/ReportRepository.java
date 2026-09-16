package com.urbano.monolith.report.repository;

import com.urbano.monolith.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    // ============================================================
    // SCOPED QUERIES (with PM Account)
    // ============================================================
    Optional<Report> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<Report> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Report> findByPmAccountIdAndType(UUID pmAccountId, String type, Pageable pageable);

    Page<Report> findByPmAccountIdAndStatus(UUID pmAccountId, String status, Pageable pageable);

    Page<Report> findByGeneratedByAndPmAccountId(UUID generatedBy, UUID pmAccountId, Pageable pageable);

    long countByPmAccountId(UUID pmAccountId);

    long countByPmAccountIdAndStatus(UUID pmAccountId, String status);

    long countByPmAccountIdAndExpiresAtBefore(UUID pmAccountId, LocalDateTime dateTime);

    // ============================================================
    // LEGACY QUERIES (deprecated)
    // ============================================================
    @Deprecated
    Page<Report> findByGeneratedByOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}