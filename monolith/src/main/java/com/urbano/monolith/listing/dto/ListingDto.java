package com.urbano.monolith.listing.dto;

import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDto {

    private UUID id;
    private UUID pmAccountId;
    private UUID propertyId;
    private UUID unitId;

    private String title;
    private String description;
    private Double price;
    private String currency;
    private Integer bedrooms;
    private Integer bathrooms;

    /**
     * Property-level classification (RESIDENTIAL / COMMERCIAL) — String.
     * Different concept from {@link #unitPropertyType}.
     */
    private String propertyType;

    private Double squareFootage;

    // ---- Commit 7: unit-level filter fields ----
    /** Fine-grained unit type (APARTMENT / HOUSE / VILLA / STUDIO / PENTHOUSE). */
    private PropertyType unitPropertyType;

    /** Whether the unit is for sale or for rent. */
    private TransactionType transactionType;

    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    private Double latitude;
    private Double longitude;

    private String status;
    private boolean published;
    private List<String> photoUrls;

    /** PM display name (Commit 6). Null if no PM_ADMIN user is linked. */
    private String pmName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}