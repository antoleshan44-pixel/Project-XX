package com.urbano.monolith.support.repository;

import com.urbano.monolith.support.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<SupportTicket> findByPmAccountIdOrderByCreatedAtDesc(UUID pmAccountId, Pageable pageable);

    Page<SupportTicket> findByPmAccountIdAndStatusOrderByCreatedAtDesc(
            UUID pmAccountId, String status, Pageable pageable);

    Optional<SupportTicket> findByIdAndUserId(UUID id, UUID userId);

    Optional<SupportTicket> findByIdAndPmAccountId(UUID id, UUID pmAccountId);
}