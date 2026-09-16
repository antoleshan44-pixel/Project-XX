package com.urbano.monolith.listing.repository;

import com.urbano.monolith.listing.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    // ✅ Scoped queries by PM Account
    Optional<Listing> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<Listing> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Listing> findByPropertyIdAndPmAccountId(UUID propertyId, UUID pmAccountId, Pageable pageable);

    Page<Listing> findByPmAccountIdAndStatus(UUID pmAccountId, String status, Pageable pageable);

    boolean existsByUnitId(UUID unitId);

    // ✅ Public queries (unscoped)
    Page<Listing> findByPublishedTrue(Pageable pageable);

    Optional<Listing> findByIdAndPublishedTrue(UUID id);

    // Legacy queries (for backward compatibility)
    @Deprecated
    Page<Listing> findByPropertyId(UUID propertyId, Pageable pageable);

    @Deprecated
    Page<Listing> findByStatus(String status, Pageable pageable);
}