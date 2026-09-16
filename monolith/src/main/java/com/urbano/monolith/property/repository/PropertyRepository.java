package com.urbano.monolith.property.repository;

import com.urbano.monolith.property.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
    Page<Property> findByOwnerId(UUID ownerId, Pageable pageable);
}
