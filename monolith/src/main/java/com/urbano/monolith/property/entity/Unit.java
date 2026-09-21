package com.urbano.monolith.property.entity;

import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.enums.UnitStatus;
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
@Table(name = "units")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String unitNumber;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private Double squareFootage;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(nullable = false)
    private Double rentAmount;

    private String currency;

    @Column(nullable = false)
    private Boolean isAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status;

    // ---- Commit 7 ----
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "property_id", insertable = false, updatable = false)
    private UUID propertyId;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String features;

    private String label;

    private boolean published;

    @ElementCollection
    @CollectionTable(name = "unit_photo_urls", joinColumns = @JoinColumn(name = "unit_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime rentedAt;

    private UUID currentTenantId;
}