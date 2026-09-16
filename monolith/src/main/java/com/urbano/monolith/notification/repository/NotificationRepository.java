package com.urbano.monolith.notification.repository;

import com.urbano.monolith.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ============================================================
    // SCOPED QUERIES (with PM Account)
    // ============================================================
    Optional<Notification> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<Notification> findByUserIdAndPmAccountIdOrderByCreatedAtDesc(
            UUID userId, UUID pmAccountId, Pageable pageable);

    List<Notification> findByUserIdAndPmAccountIdAndIsReadFalseOrderByCreatedAtDesc(
            UUID userId, UUID pmAccountId);

    long countByUserIdAndPmAccountIdAndIsReadFalse(UUID userId, UUID pmAccountId);

    long countByPmAccountId(UUID pmAccountId);

    long countByPmAccountIdAndStatus(UUID pmAccountId, String status);

    long countByPmAccountIdAndIsReadTrue(UUID pmAccountId);

    long countByPmAccountIdAndIsReadFalse(UUID pmAccountId);

    // ============================================================
    // LEGACY QUERIES (deprecated)
    // ============================================================
    @Deprecated
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Deprecated
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    @Deprecated
    long countByUserIdAndIsReadFalse(UUID userId);
}