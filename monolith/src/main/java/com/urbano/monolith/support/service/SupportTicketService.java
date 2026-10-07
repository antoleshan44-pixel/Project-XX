package com.urbano.monolith.support.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.UserRole;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.support.dto.*;
import com.urbano.monolith.support.entity.SupportTicket;
import com.urbano.monolith.support.entity.SupportTicketReply;
import com.urbano.monolith.support.repository.SupportTicketReplyRepository;
import com.urbano.monolith.support.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketReplyRepository replyRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketDto createTicket(CreateTicketRequest request) {
        UUID userId = requireUserId();
        UUID pmAccountId = TenantContext.getPmAccountId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = SupportTicket.builder()
                .pmAccountId(pmAccountId)
                .userId(userId)
                .userName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status("OPEN")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ticket = ticketRepository.save(ticket);
        log.info("Support ticket created: {} by {}", ticket.getId(), user.getEmail());
        return toDto(ticket, List.of());
    }

    @Transactional(readOnly = true)
    public TicketDto getTicket(UUID id) {
        SupportTicket ticket = loadTicketScopedToCaller(id);
        List<SupportTicketReply> replies = replyRepository.findByTicketIdOrderByCreatedAtAsc(id);
        return toDto(ticket, replies);
    }

    @Transactional(readOnly = true)
    public Page<TicketDto> listMyTickets(int page, int size) {
        UUID userId = requireUserId();
        UserRole role = TenantContext.getUserRole();
        UUID pmAccountId = TenantContext.getPmAccountId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));

        Page<SupportTicket> tickets;
        if (role == UserRole.PM_ADMIN || role == UserRole.PM_STAFF) {
            tickets = pmAccountId != null
                    ? ticketRepository.findByPmAccountIdOrderByCreatedAtDesc(pmAccountId, pageable)
                    : ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            tickets = ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return tickets.map(t -> toDto(t, List.of()));
    }

    @Transactional
    public TicketDto reply(UUID ticketId, TicketReplyRequest request) {
        UUID userId = requireUserId();
        SupportTicket ticket = loadTicketScopedToCaller(ticketId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicketReply reply = SupportTicketReply.builder()
                .ticketId(ticketId)
                .senderUserId(userId)
                .senderName(user.getFirstName() + " " + user.getLastName())
                .senderRole(user.getRole().name())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
        replyRepository.save(reply);

        ticket.setUpdatedAt(LocalDateTime.now());
        if ("OPEN".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }
        ticketRepository.save(ticket);

        List<SupportTicketReply> replies = replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return toDto(ticket, replies);
    }

    // ============================================================
    // Helpers
    // ============================================================
    private UUID requireUserId() {
        UUID userId = TenantContext.getUserId();
        if (userId == null) throw new UnauthorizedException("Authentication required");
        return userId;
    }

    private SupportTicket loadTicketScopedToCaller(UUID id) {
        UUID userId = requireUserId();
        UserRole role = TenantContext.getUserRole();
        UUID pmAccountId = TenantContext.getPmAccountId();

        // SUPER_ADMIN can see any ticket.
        if (role == UserRole.SUPER_ADMIN) {
            return ticketRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        }

        // PM sees any ticket in their account.
        if ((role == UserRole.PM_ADMIN || role == UserRole.PM_STAFF) && pmAccountId != null) {
            return ticketRepository.findByIdAndPmAccountId(id, pmAccountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        }

        // Tenant sees only their own.
        return ticketRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private TicketDto toDto(SupportTicket t, List<SupportTicketReply> replies) {
        return TicketDto.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .userName(t.getUserName())
                .userEmail(t.getUserEmail())
                .subject(t.getSubject())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .replies(replies.stream().map(r -> TicketReplyDto.builder()
                        .id(r.getId())
                        .senderUserId(r.getSenderUserId())
                        .senderName(r.getSenderName())
                        .senderRole(r.getSenderRole())
                        .message(r.getMessage())
                        .createdAt(r.getCreatedAt())
                        .build()).toList())
                .build();
    }
}