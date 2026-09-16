package com.urbano.monolith.audit.repository;

import com.urbano.monolith.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Find by user ID
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    // Find by event type
    Page<AuditLog> findByEventType(String eventType, Pageable pageable);

    // Find by action
    Page<AuditLog> findByAction(String action, Pageable pageable);

    // Find by service name
    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    // Find by timestamp after
    Page<AuditLog> findByTimestampAfter(LocalDateTime timestamp, Pageable pageable);

    // Find failed logs
    Page<AuditLog> findByIsSuccessFalse(Pageable pageable);

    // Custom query: find by user ID and event type
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.eventType = :eventType")
    Page<AuditLog> findByUserIdAndEventType(@Param("userId") String userId, @Param("eventType") String eventType, Pageable pageable);

    // Custom query: find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    // Count by event type
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.eventType = :eventType")
    long countByEventType(@Param("eventType") String eventType);

    // Count failed logs by service
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.serviceName = :serviceName AND a.isSuccess = false")
    long countFailedByService(@Param("serviceName") String serviceName);
}