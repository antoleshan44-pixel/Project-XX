package com.urbano.monolith.notification.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.NotificationDeliveryException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.common.exception.ValidationException;
import com.urbano.monolith.notification.dto.NotificationDto;
import com.urbano.monolith.notification.dto.NotificationRequest;
import com.urbano.monolith.notification.dto.NotificationResponse;
import com.urbano.monolith.notification.dto.NotificationStatsDto;
import com.urbano.monolith.notification.entity.Notification;
import com.urbano.monolith.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Sending notification: type={}, channel={}, recipient={}",
                request.getType(), request.getChannel(), maskRecipient(request.getRecipient()));

        Notification notification = Notification.builder()
                .pmAccountId(request.getPmAccountId())
                .userId(request.getUserId())
                .type(request.getType())
                .channel(request.getChannel())
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .content(request.getContent())
                .status("PENDING")
                .isRead(false)
                .deliveryAttempts(0)
                .build();

        notification = notificationRepository.save(notification);

        try {
            String channel = request.getChannel().toUpperCase();
            switch (channel) {
                case "EMAIL" -> {
                    emailService.sendNotificationEmail(request);
                    notification.setStatus("SENT");
                }
                case "SMS" -> {
                    smsService.sendNotificationSms(request);
                    notification.setStatus("SENT");
                }
                case "PUSH" -> {
                    pushNotificationService.sendPushNotification(request);
                    notification.setStatus("SENT");
                }
                case "IN_APP" -> {
                    notification.setStatus("DELIVERED");
                    notification.setSentAt(LocalDateTime.now());
                }
                default -> throw new ValidationException("Unsupported channel: " + channel);
            }

            notification.setSentAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);

            return NotificationResponse.builder()
                    .id(notification.getId())
                    .status(notification.getStatus())
                    .message("Notification sent successfully")
                    .sentAt(notification.getSentAt())
                    .channel(request.getChannel())
                    .type(request.getType())
                    .build();

        } catch (ValidationException e) {
            // Bad input — save failure state and rethrow the typed exception (400)
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            notification.setDeliveryAttempts(notification.getDeliveryAttempts() + 1);
            notificationRepository.save(notification);
            throw e;
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            notification.setDeliveryAttempts(notification.getDeliveryAttempts() + 1);
            notificationRepository.save(notification);
            throw new NotificationDeliveryException(
                    "Notification sending failed: " + e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void sendNotificationAsync(NotificationRequest request) {
        try {
            sendNotification(request);
        } catch (Exception e) {
            log.error("Async notification failed: {}", e.getMessage());
        }
    }

    public NotificationDto getNotification(UUID id, UUID pmAccountId) {
        Notification notification = notificationRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return mapToDto(notification);
    }

    public PagedResponse<NotificationDto> getUserNotifications(UUID userId, UUID pmAccountId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdAndPmAccountIdOrderByCreatedAtDesc(
                        userId, pmAccountId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
        return mapToPagedResponse(notifications);
    }

    public List<NotificationDto> getUnreadNotifications(UUID userId, UUID pmAccountId) {
        return notificationRepository
                .findByUserIdAndPmAccountIdAndIsReadFalseOrderByCreatedAtDesc(userId, pmAccountId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(UUID userId, UUID pmAccountId) {
        return notificationRepository.countByUserIdAndPmAccountIdAndIsReadFalse(userId, pmAccountId);
    }

    @Transactional
    public NotificationDto markAsRead(UUID id, UUID pmAccountId) {
        Notification notification = notificationRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notification.setStatus("READ");
        notification = notificationRepository.save(notification);
        return mapToDto(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId, UUID pmAccountId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndPmAccountIdAndIsReadFalseOrderByCreatedAtDesc(userId, pmAccountId);
        notifications.forEach(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            n.setStatus("READ");
        });
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void deleteNotification(UUID id, UUID pmAccountId) {
        Notification notification = notificationRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public NotificationStatsDto getStats(UUID pmAccountId) {
        long totalSent = notificationRepository.countByPmAccountId(pmAccountId);
        long totalDelivered = notificationRepository.countByPmAccountIdAndStatus(pmAccountId, "SENT");
        long totalRead = notificationRepository.countByPmAccountIdAndIsReadTrue(pmAccountId);
        long totalFailed = notificationRepository.countByPmAccountIdAndStatus(pmAccountId, "FAILED");
        long pendingCount = notificationRepository.countByPmAccountIdAndStatus(pmAccountId, "PENDING");
        long unreadCount = notificationRepository.countByPmAccountIdAndIsReadFalse(pmAccountId);

        return NotificationStatsDto.builder()
                .totalSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalRead(totalRead)
                .totalFailed(totalFailed)
                .pendingCount(pendingCount)
                .unreadCount(unreadCount)
                .build();
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .pmAccountId(notification.getPmAccountId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .status(notification.getStatus())
                .isRead(notification.isRead())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .deliveryAttempts(notification.getDeliveryAttempts())
                .errorMessage(notification.getErrorMessage())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    private PagedResponse<NotificationDto> mapToPagedResponse(Page<Notification> page) {
        List<NotificationDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<NotificationDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private static String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 4) return "***";
        return "***" + recipient.substring(recipient.length() - 4);
    }
}