package com.urbano.monolith.property.dto;

import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Flattened view of a vacant, published unit — carries both unit-level and
 * property-level fields, plus the resolved PM display name.
 *
 * <p>Consumed by {@code PublicListingService} (listing module) and returned
 * by {@code UnitService.getVacantPublishedUnitDtos()} (property module).
 * This is the single canonical vacant-unit DTO for public listings.</p>
 *
 * <p>Note on "type" fields — two distinct concepts, do not merge:</p>
 * <ul>
 *   <li>{@link #unitPropertyType} — unit-level enum (APARTMENT / HOUSE / …),
 *       set per unit, used by the mobile filter sheet.</li>
 *   <li>{@link #propertyType} — property-level String (RESIDENTIAL /
 *       COMMERCIAL), inherited from {@code Property.type}.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacantUnitDto {

    // ---- Unit identity / core fields ----
    private UUID id;
    private UUID propertyId;
    private String unitNumber;
    private String label;
    private Integer floor;
    private Double squareFootage;
    private Integer bedrooms;
    private Integer bathrooms;

    /** BigDecimal internally; ListingDto exposes it as Double. */
    private BigDecimal rentAmount;
    private String currency;

    private String description;
    private String features;
    private List<String> photoUrls;

    /** First photo URL — convenience for card views. */
    private String thumbnailUrl;

    private String status;
    private boolean published;

    // ---- Commit 7: unit-level filtering fields ----
    /**
     * Fine-grained unit type (APARTMENT / HOUSE / VILLA / STUDIO / PENTHOUSE).
     * Distinct from {@link #propertyType} (property-level String).
     */
    private PropertyType unitPropertyType;

    /**
     * Whether this unit is for sale or for rent. Mobile displays this on
     * every property card and detail screen so a price is always readable
     * in context.
     */
    private TransactionType transactionType;

    // ---- Property-level fields (denormalized from property.entity) ----
    private String propertyName;

    /**
     * Property-level classification (RESIDENTIAL / COMMERCIAL).
     * Different concept from {@link #unitPropertyType}.
     */
    private String propertyType;

    private String address;
    private String city;
    private String state;
    private String country;
    private String location;

    // ---- PM ownership ----
    private UUID pmAccountId;

    /**
     * Resolved display name of the PM_ADMIN user owning {@link #pmAccountId}.
     * Null if the PM account has no PM_ADMIN user yet, or if the property has
     * no PM account (orphaned row). Mobile falls back to a generic label.
     */
    private String pmName;
}