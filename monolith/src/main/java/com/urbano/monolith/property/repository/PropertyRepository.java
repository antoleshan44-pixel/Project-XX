package com.urbano.monolith.property.repository;

import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.monolith.property.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    // ---------------------------------------------------------------
    // Existing PM-scoped queries
    // ---------------------------------------------------------------
    Page<Property> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Property> findByOwnerIdAndPmAccountId(UUID ownerId, UUID pmAccountId, Pageable pageable);

    // ---------------------------------------------------------------
    // Platform approval queries (admin + public listing gate)
    // ---------------------------------------------------------------
    Page<Property> findByApprovalStatus(PropertyApprovalStatus status, Pageable pageable);

    Page<Property> findByApprovalStatusAndPmAccountId(
            PropertyApprovalStatus status, UUID pmAccountId, Pageable pageable);

    long countByApprovalStatus(PropertyApprovalStatus status);

    // ---------------------------------------------------------------
    // Dashboard — aggregation pushed to SQL
    // ---------------------------------------------------------------
    long countByPmAccountId(UUID pmAccountId);

    /**
     * One row per property:
     * [0] propertyId (UUID)
     * [1] property name (String)
     * [2] occupied unit count (Long)
     * [3] total unit count (Long)
     */
    @Query("""
        SELECT p.id, p.name,
               SUM(CASE WHEN u.status IN (com.urbano.common.enums.UnitStatus.OCCUPIED,
                                          com.urbano.common.enums.UnitStatus.RENTED)
                        THEN 1 ELSE 0 END),
               COUNT(u)
        FROM Property p
        LEFT JOIN Unit u ON u.property.id = p.id AND u.deletedAt IS NULL
        WHERE p.pmAccountId = :pmAccountId
        GROUP BY p.id, p.name
        ORDER BY p.name ASC
    """)
    List<Object[]> findOccupancyByProperty(@Param("pmAccountId") UUID pmAccountId);
}