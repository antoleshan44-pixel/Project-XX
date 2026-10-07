package com.urbano.monolith.newsletter.controller;

import com.urbano.monolith.newsletter.dto.NewsletterSubscribeRequest;
import com.urbano.monolith.newsletter.dto.NewsletterSubscribeResponse;
import com.urbano.monolith.newsletter.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/newsletter")
@RequiredArgsConstructor
public class PublicNewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<NewsletterSubscribeResponse> subscribe(
            @Valid @RequestBody NewsletterSubscribeRequest request) {
        return ResponseEntity.ok(newsletterService.subscribe(request));
    }
}