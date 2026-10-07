package com.urbano.monolith.support.repository;

import com.urbano.monolith.support.entity.SupportTicketReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, UUID> {
    List<SupportTicketReply> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}