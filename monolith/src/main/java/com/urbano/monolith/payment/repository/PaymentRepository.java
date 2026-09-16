package com.urbano.monolith.payment.repository;

import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // ============================================================
    // BASIC QUERIES
    // ============================================================
    Optional<Payment> findByReferenceNumber(String referenceNumber);

    Optional<Payment> findByMpesaReceiptNumber(String mpesaReceiptNumber);

    boolean existsByMpesaReceiptNumber(String mpesaReceiptNumber);

    List<Payment> findByIsReconciledFalse();

    List<Payment> findByReconciledAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    // ============================================================
    // TENANT QUERIES
    // ============================================================
    Page<Payment> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<Payment> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, PaymentStatus status, Pageable pageable);

    List<Payment> findByTenantIdAndStatus(UUID tenantId, PaymentStatus status);

    // ============================================================
    // PROPERTY QUERIES
    // ============================================================
    Page<Payment> findByPropertyIdOrderByCreatedAtDesc(UUID propertyId, Pageable pageable);

    // ============================================================
    // UNIT QUERIES
    // ============================================================
    Page<Payment> findByUnitIdOrderByCreatedAtDesc(UUID unitId, Pageable pageable);

    List<Payment> findByUnitId(UUID unitId);

    // ============================================================
    // PM ACCOUNT SCOPED QUERIES
    // ============================================================
    Page<Payment> findByPmAccountIdOrderByTransactionDateDesc(UUID pmAccountId, Pageable pageable);

    Page<Payment> findByPmAccountIdAndStatusOrderByTransactionDateDesc(UUID pmAccountId, PaymentStatus status, Pageable pageable);

    Page<Payment> findByPmAccountIdAndTenantIdOrderByTransactionDateDesc(UUID pmAccountId, UUID tenantId, Pageable pageable);

    // ✅ Fixed: This method signature matches what AdminPaymentService expects
    @Query("SELECT p FROM Payment p WHERE p.pmAccountId = :pmAccountId " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:startDate IS NULL OR p.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR p.transactionDate <= :endDate)")
    Page<Payment> findByPmAccountIdWithFilters(
            @Param("pmAccountId") UUID pmAccountId,
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // ============================================================
    // STATUS QUERIES
    // ============================================================
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.pmAccountId = :pmAccountId AND p.status = :status")
    long countByPmAccountIdAndStatus(@Param("pmAccountId") UUID pmAccountId, @Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.pmAccountId = :pmAccountId AND p.status = :status")
    BigDecimal sumAmountByPmAccountIdAndStatus(@Param("pmAccountId") UUID pmAccountId, @Param("status") PaymentStatus status);

    // ============================================================
    // DATE RANGE QUERIES
    // ============================================================
    List<Payment> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    // ============================================================
    // ADMIN QUERIES (unscoped)
    // ============================================================
    Page<Payment> findAll(Pageable pageable);
}