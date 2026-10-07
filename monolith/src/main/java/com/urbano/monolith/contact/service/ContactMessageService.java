package com.urbano.monolith.contact.service;

import com.urbano.common.enums.UserRole;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public ContactMessageResponse processContactSubmission(ContactMessageRequest request) {
        log.info("Processing public contact submission from {} ({})",
                request.getFullName(), request.getEmail());

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

        notifyPlatformAdmins(message);

        return ContactMessageResponse.builder()
                .id(message.getId())
                .status(message.getStatus())
                .message("Thank you for contacting Urbano Homes. Your message has been received.")
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * Notify all SUPER_ADMIN users + PM_ADMINs of the platform.
     * If you later scope contact submissions to a specific PM (e.g. by
     * propertyId in the request), switch to that PM account here.
     */
    private void notifyPlatformAdmins(ContactMessage message) {
        List<User> recipients;
        try {
            recipients = userRepository.findByRole(UserRole.SUPER_ADMIN);
            // Also include PM_ADMINs so a PM contact page also reaches them.
            recipients.addAll(userRepository.findByRole(UserRole.PM_ADMIN));
        } catch (Exception e) {
            log.warn("Could not resolve contact recipients: {}", e.getMessage());
            return;
        }

        for (User recipient : recipients) {
            try {
                NotificationRequest notifReq = NotificationRequest.builder()
                        .pmAccountId(recipient.getPmAccountId())
                        .userId(recipient.getId())
                        .type("GENERAL_INQUIRY")
                        .channel("EMAIL")
                        .recipient(recipient.getEmail())
                        .subject("New Contact Form Message: " + message.getSubject())
                        .content(String.format(
                                "New contact submission\n\nFrom: %s <%s>%s\nSubject: %s\n\n%s",
                                message.getFullName(),
                                message.getEmail(),
                                message.getPhone() != null ? " (" + message.getPhone() + ")" : "",
                                message.getSubject(),
                                message.getMessage()))
                        .build();
                notificationService.sendNotificationAsync(notifReq);
            } catch (Exception e) {
                log.warn("Failed to notify {} about contact message {}: {}",
                        recipient.getEmail(), message.getId(), e.getMessage());
            }
        }
    }
}