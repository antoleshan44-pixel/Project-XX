package com.urbano.monolith.newsletter.service;

import com.urbano.monolith.newsletter.dto.NewsletterSubscribeRequest;
import com.urbano.monolith.newsletter.dto.NewsletterSubscribeResponse;
import com.urbano.monolith.newsletter.entity.NewsletterSubscriber;
import com.urbano.monolith.newsletter.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;

    @Transactional
    public NewsletterSubscribeResponse subscribe(NewsletterSubscribeRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        var existing = repository.findByEmail(email);
        if (existing.isPresent()) {
            NewsletterSubscriber sub = existing.get();
            if (!Boolean.TRUE.equals(sub.getIsActive())) {
                sub.setIsActive(true);
                repository.save(sub);
                log.info("Newsletter subscription reactivated for {}", email);
                return NewsletterSubscribeResponse.builder()
                        .message("Welcome back! You are subscribed again.")
                        .alreadySubscribed(false)
                        .build();
            }
            return NewsletterSubscribeResponse.builder()
                    .message("You are already subscribed.")
                    .alreadySubscribed(true)
                    .build();
        }

        repository.save(NewsletterSubscriber.builder()
                .email(email)
                .isActive(true)
                .build());
        log.info("Newsletter subscription created for {}", email);

        return NewsletterSubscribeResponse.builder()
                .message("Thanks for subscribing!")
                .alreadySubscribed(false)
                .build();
    }
}