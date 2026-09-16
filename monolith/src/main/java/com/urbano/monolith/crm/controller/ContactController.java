package com.urbano.monolith.crm.controller;

import com.urbano.common.dto.PagedResponse;
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

    // ============================================================
    // CREATE CONTACT
    // ============================================================
    @PostMapping
    public ResponseEntity<ContactDto> createContact(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody ContactRequest request) {
        // ✅ Ensure PM Account from header matches request
        request.setPmAccountId(pmAccountId);
        return ResponseEntity.ok(contactService.createContact(request));
    }

    // ============================================================
    // GET CONTACT BY ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<ContactDto> getContact(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(contactService.getContact(id, pmAccountId));
    }

    // ============================================================
    // GET ALL CONTACTS (Paginated)
    // ============================================================
    @GetMapping
    public ResponseEntity<PagedResponse<ContactDto>> getAllContacts(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.getAllContacts(pmAccountId, page, size));
    }

    // ============================================================
    // GET ACTIVE CONTACTS ONLY
    // ============================================================
    @GetMapping("/active")
    public ResponseEntity<PagedResponse<ContactDto>> getActiveContacts(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.getActiveContacts(pmAccountId, page, size));
    }

    // ============================================================
    // GET CONTACTS BY TYPE
    // ============================================================
    @GetMapping("/type/{type}")
    public ResponseEntity<PagedResponse<ContactDto>> getContactsByType(
            @PathVariable("type") String type,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.getContactsByType(pmAccountId, type, page, size));
    }

    // ============================================================
    // SEARCH CONTACTS
    // ============================================================
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ContactDto>> searchContacts(
            @RequestParam("query") String query,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.searchContacts(pmAccountId, query, page, size));
    }

    // ============================================================
    // UPDATE CONTACT
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<ContactDto> updateContact(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, pmAccountId, request));
    }

    // ============================================================
    // DELETE CONTACT (Soft Delete)
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        contactService.deleteContact(id, pmAccountId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // UPDATE CONTACT STATUS
    // ============================================================
    @PatchMapping("/{id}/status")
    public ResponseEntity<ContactDto> updateContactStatus(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam("active") boolean active) {
        return ResponseEntity.ok(contactService.updateContactStatus(id, pmAccountId, active));
    }

    // ============================================================
    // UPDATE LAST CONTACT DATE
    // ============================================================
    @PostMapping("/{id}/last-contact")
    public ResponseEntity<ContactDto> updateLastContactDate(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(contactService.updateLastContactDate(id, pmAccountId));
    }
}