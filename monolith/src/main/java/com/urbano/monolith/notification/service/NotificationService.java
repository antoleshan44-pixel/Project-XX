package com.urbano.monolith.notification.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
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

    /**
     * Send notification (synchronous)
     */
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Sending notification: type={}, channel={}, recipient={}",
                request.getType(), request.getChannel(), request.getRecipient());

        // Create notification record
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
            // Send based on channel
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
                    // Just save as read for in-app
                    notification.setStatus("DELIVERED");
                    notification.setSentAt(LocalDateTime.now());
                }
                default -> throw new RuntimeException("Unsupported channel: " + channel);
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

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            notification.setDeliveryAttempts(notification.getDeliveryAttempts() + 1);
            notificationRepository.save(notification);
            throw new RuntimeException("Notification sending failed: " + e.getMessage());
        }
    }

    /**
     * Send notification asynchronously
     */
    @Async
    @Transactional
    public void sendNotificationAsync(NotificationRequest request) {
        try {
            sendNotification(request);
        } catch (Exception e) {
            log.error("Async notification failed: {}", e.getMessage());
        }
    }

    /**
     * Get notification by ID - Scoped to PM Account
     */
    public NotificationDto getNotification(UUID id, UUID pmAccountId) {
        Notification notification = notificationRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return mapToDto(notification);
    }

    /**
     * Get all notifications for a user - Scoped to PM Account
     */
    public PagedResponse<NotificationDto> getUserNotifications(UUID userId, UUID pmAccountId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdAndPmAccountIdOrderByCreatedAtDesc(
                        userId, pmAccountId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
        return mapToPagedResponse(notifications);
    }

    /**
     * Get unread notifications for a user
     */
    public List<NotificationDto> getUnreadNotifications(UUID userId, UUID pmAccountId) {
        return notificationRepository
                .findByUserIdAndPmAccountIdAndIsReadFalseOrderByCreatedAtDesc(userId, pmAccountId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unread count for a user
     */
    public long getUnreadCount(UUID userId, UUID pmAccountId) {
        return notificationRepository.countByUserIdAndPmAccountIdAndIsReadFalse(userId, pmAccountId);
    }

    /**
     * Mark notification as read - Scoped to PM Account
     */
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

    /**
     * Mark all notifications as read for a user
     */
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

    /**
     * Delete notification - Scoped to PM Account
     */
    @Transactional
    public void deleteNotification(UUID id, UUID pmAccountId) {
        Notification notification = notificationRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * Get notification statistics - Scoped to PM Account
     */
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

    // ============================================================
    // MAPPER METHODS
    // ============================================================
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
}