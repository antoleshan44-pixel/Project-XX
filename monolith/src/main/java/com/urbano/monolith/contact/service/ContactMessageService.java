package com.urbano.monolith.contact.service;

import com.urbano.monolith.contact.dto.ContactMessageRequest;
import com.urbano.monolith.contact.dto.ContactMessageResponse;
import com.urbano.monolith.contact.entity.ContactMessage;
import com.urbano.monolith.contact.repository.ContactMessageRepository;
import com.urbano.monolith.notification.dto.NotificationRequest;
import com.urbano.monolith.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final NotificationService notificationService;

    @Transactional
    public ContactMessageResponse processContactSubmission(ContactMessageRequest request) {
        log.info("Processing public contact submission from {} ({})", request.getFullName(), request.getEmail());

        ContactMessage message = ContactMessage.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status("NEW")
                .createdAt(LocalDateTime.now())
                .build();

        message = contactMessageRepository.save(message);

        // Notify property manager / admin team asynchronously via email
        try {
            NotificationRequest notifReq = NotificationRequest.builder()
                    .type("GENERAL_INQUIRY")
                    .channel("EMAIL")
                    .recipient("admin@urbano.homes")
                    .subject("New Contact Form Message: " + request.getSubject())
                    .content(String.format("New message received from %s (%s, %s):\n\n%s",
                            request.getFullName(), request.getEmail(),
                            request.getPhone() != null ? request.getPhone() : "N/A",
                            request.getMessage()))
                    .build();

            notificationService.sendNotificationAsync(notifReq);
        } catch (Exception e) {
            log.warn("Failed to dispatch admin notification for contact message {}: {}", message.getId(), e.getMessage());
        }

        return ContactMessageResponse.builder()
                .id(message.getId())
                .status(message.getStatus())
                .message("Thank you for contacting Urbano Homes. Your message has been received.")
                .createdAt(message.getCreatedAt())
                .build();
    }
}
