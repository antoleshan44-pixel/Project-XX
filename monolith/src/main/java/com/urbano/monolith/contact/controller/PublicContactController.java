package com.urbano.monolith.contact.controller;

import com.urbano.monolith.contact.dto.ContactMessageRequest;
import com.urbano.monolith.contact.dto.ContactMessageResponse;
import com.urbano.monolith.contact.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/contact")
@RequiredArgsConstructor
public class PublicContactController {

    private final ContactMessageService contactMessageService;

    @PostMapping
    public ResponseEntity<ContactMessageResponse> submitContactForm(
            @Valid @RequestBody ContactMessageRequest request) {
        return ResponseEntity.ok(contactMessageService.processContactSubmission(request));
    }
}
