package com.urbano.monolith.property.entity;

import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.common.enums.PropertyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

    private String city;

    private String state;

    private String zipCode;

    private String country;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer totalUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status;

    @Column(nullable = false)
    private UUID ownerId;

    private String ownerName;

    private String ownerEmail;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private Double latitude;

    private Double longitude;

    // Additional fields for service compatibility
    private String location;

    private UUID pmAccountId;

    private LocalDateTime deletedAt;

    // ============================================================
    // Phase 2 — platform approval workflow (Model B)
    // ============================================================
    // Every property created by a PM starts PENDING_APPROVAL and is
    // hidden from tenant-facing listings until a SUPER_ADMIN approves it.

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    private PropertyApprovalStatus approvalStatus = PropertyApprovalStatus.PENDING_APPROVAL;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "rejection_reason", length = 512)
    private String rejectionReason;

    @Column(name = "suspended_reason", length = 512)
    private String suspendedReason;

    // ============================================================

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Unit> units = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}