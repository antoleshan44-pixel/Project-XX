package com.urbano.monolith.property.repository;

import com.urbano.monolith.property.entity.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {
    Page<Unit> findByPropertyId(UUID propertyId, Pageable pageable);
    Page<Unit> findByIsAvailableTrue(Pageable pageable);
    List<Unit> findByIsAvailableTrueAndPublishedTrue();
}
