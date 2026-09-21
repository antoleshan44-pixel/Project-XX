package com.urbano.monolith.property.repository;

import com.urbano.common.enums.ViewingStatus;
import com.urbano.monolith.property.entity.Viewing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewingRepository extends JpaRepository<Viewing, UUID> {

    // ---- PM-scoped ----
    Optional<Viewing> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    Page<Viewing> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Viewing> findByPmAccountIdAndStatus(UUID pmAccountId, ViewingStatus status, Pageable pageable);

    Page<Viewing> findByUnitIdAndPmAccountId(UUID unitId, UUID pmAccountId, Pageable pageable);

    // ---- User-scoped (future GET /users/{userId}/viewings) ----
    Page<Viewing> findByRequestedByUserId(UUID userId, Pageable pageable);
}