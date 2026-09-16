package com.urbano.monolith.crm.entity;

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
@Table(name = "crm_contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pm_account_id", nullable = false)
    private UUID pmAccountId;  // ✅ Added - critical for scoping

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    private String mobile;

    @Column(nullable = false)
    private String type; // TENANT, PROPERTY_OWNER, VENDOR, EMPLOYEE, PROSPECT, LANDLORD

    private String company;

    private String position;

    private String address;

    private String city;

    private String state;

    private String zipCode;

    private String country;

    @Column(name = "preferred_contact_method")
    private String preferredContactMethod; // EMAIL, PHONE, SMS, NONE

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "last_contact_date")
    private LocalDateTime lastContactDate;

    private String source; // WEBSITE, REFERRAL, SOCIAL_MEDIA, OTHER, LISTING_INQUIRY

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Helper method to get full name
    public String getFullName() {
        return firstName + " " + lastName;
    }
}