package com.urbano.monolith.crm.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.crm.dto.ContactDto;
import com.urbano.monolith.crm.dto.ContactRequest;
import com.urbano.monolith.crm.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/crm/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    public ResponseEntity<ContactDto> createContact(@Valid @RequestBody ContactRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.ok(contactService.createContact(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactDto> getContact(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(contactService.getContact(id, requireTenant()));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ContactDto>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.getAllContacts(requireTenant(), page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<ContactDto>> getActiveContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.getActiveContacts(requireTenant(), page, size));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<PagedResponse<ContactDto>> getContactsByType(
            @PathVariable("type") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.getContactsByType(requireTenant(), type, page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ContactDto>> searchContacts(
            @RequestParam("query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.searchContacts(requireTenant(), query, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactDto> updateContact(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, requireTenant(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable("id") UUID id) {
        contactService.deleteContact(id, requireTenant());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ContactDto> updateContactStatus(
            @PathVariable("id") UUID id,
            @RequestParam("active") boolean active) {
        return ResponseEntity.ok(contactService.updateContactStatus(id, requireTenant(), active));
    }

    @PostMapping("/{id}/last-contact")
    public ResponseEntity<ContactDto> updateLastContactDate(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(contactService.updateLastContactDate(id, requireTenant()));
    }
}