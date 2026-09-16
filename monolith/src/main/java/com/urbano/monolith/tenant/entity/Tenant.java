package com.urbano.monolith.tenant.entity;

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
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pm_account_id", nullable = false)
    private UUID pmAccountId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name")
    private String fullName;

    // ✅ NEW: First name (NOT NULL in DB)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    // ✅ NEW: Last name (NOT NULL in DB)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "emergency_phone")
    private String emergencyPhone;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "credit_balance")
    @Builder.Default
    private Double creditBalance = 0.0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Lease> leases = new ArrayList<>();

    // ✅ Helper methods
    public String getFullName() {
        return fullName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}