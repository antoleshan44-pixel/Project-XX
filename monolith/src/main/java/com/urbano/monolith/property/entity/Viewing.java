package com.urbano.monolith.property.entity;

import com.urbano.common.enums.ViewingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "viewings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Viewing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    /** Denormalized from the unit's property at creation time, for PM scoping. */
    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    /** Denormalized from the unit's property at creation time, for PM scoping. */
    @Column(name = "pm_account_id", nullable = false)
    private UUID pmAccountId;

    @Column(name = "requested_by_name", nullable = false)
    private String requestedByName;

    @Column(name = "requested_by_phone", nullable = false)
    private String requestedByPhone;

    /**
     * Set only when the requester is authenticated (the common mobile case).
     * Null for anonymous requests from a prospective renter browsing listings.
     * Enables a future {@code GET /users/{userId}/viewings}.
     */
    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ViewingStatus status = ViewingStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ---- Helpers ----
    public boolean isActive() {
        return status == ViewingStatus.SCHEDULED && deletedAt == null;
    }
}