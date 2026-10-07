package com.urbano.monolith.newsletter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterSubscribeResponse {
    private String message;
    private boolean alreadySubscribed;
}