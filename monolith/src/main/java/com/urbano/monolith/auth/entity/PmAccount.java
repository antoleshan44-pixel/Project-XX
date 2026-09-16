package com.urbano.monolith.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pm_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "service_option", nullable = false)
    @Builder.Default
    private String serviceOption = "SOFTWARE_ONLY";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ✅ FIX: Let the DATABASE handle timestamps with DEFAULT CURRENT_TIMESTAMP
    // Do NOT use @CreationTimestamp or @UpdateTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}