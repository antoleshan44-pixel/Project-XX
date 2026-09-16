package com.urbano.monolith.listing.entity;

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
@Table(name = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pm_account_id", nullable = false)
    private UUID pmAccountId;  // ✅ Added for scoping

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    private String currency;

    private Integer bedrooms;

    private Integer bathrooms;  // ✅ Added

    @Column(name = "property_type")
    private String propertyType;  // ✅ Added for filtering

    @Column(name = "square_footage")
    private Double squareFootage;

    private String address;

    private String city;

    private String state;

    private String zipCode;

    private String country;

    private Double latitude;  // ✅ Added

    private Double longitude;  // ✅ Added

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "photo_urls")
    private String photoUrls;  // ✅ Added - comma separated URLs

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}