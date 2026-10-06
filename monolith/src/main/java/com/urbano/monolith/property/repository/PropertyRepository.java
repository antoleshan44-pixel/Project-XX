package com.urbano.monolith.property.repository;

import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.monolith.property.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    // ---------------------------------------------------------------
    // Existing PM-scoped queries
    // ---------------------------------------------------------------

    Page<Property> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Property> findByOwnerIdAndPmAccountId(UUID ownerId, UUID pmAccountId, Pageable pageable);

    // ---------------------------------------------------------------
    // Phase 2 — platform approval queries (admin + public listing gate)
    // ---------------------------------------------------------------

    Page<Property> findByApprovalStatus(PropertyApprovalStatus status, Pageable pageable);

    Page<Property> findByApprovalStatusAndPmAccountId(
            PropertyApprovalStatus status, UUID pmAccountId, Pageable pageable);

    long countByApprovalStatus(PropertyApprovalStatus status);
}