package com.urbano.monolith.notification.service;

import com.urbano.monolith.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;  // ✅ ADD THIS IMPORT

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send push notification via WebSocket
     */
    public void sendPushNotification(NotificationRequest request) {
        try {
            String destination = "/topic/notifications/" + request.getUserId();
            messagingTemplate.convertAndSend(destination, request);
            log.info("Push notification sent to user: {}", request.getUserId());
        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}",
                    request.getUserId(), e.getMessage());
            throw new RuntimeException("Push notification failed", e);
        }
    }

    /**
     * Send notification to a specific user session
     */
    public void sendToUser(UUID userId, Object payload) {
        String destination = "/queue/notifications/" + userId;
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
    }
}