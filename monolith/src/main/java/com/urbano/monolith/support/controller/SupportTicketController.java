package com.urbano.monolith.support.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.support.dto.CreateTicketRequest;
import com.urbano.monolith.support.dto.TicketDto;
import com.urbano.monolith.support.dto.TicketReplyRequest;
import com.urbano.monolith.support.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService service;

    @PostMapping
    public ResponseEntity<TicketDto> create(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.ok(service.createTicket(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TicketDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TicketDto> result = service.listMyTickets(page, size);
        return ResponseEntity.ok(PagedResponse.<TicketDto>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getTicket(id));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<TicketDto> reply(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TicketReplyRequest request) {
        return ResponseEntity.ok(service.reply(id, request));
    }
}