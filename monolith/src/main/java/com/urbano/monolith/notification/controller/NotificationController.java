package com.urbano.monolith.notification.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.notification.dto.NotificationDto;
import com.urbano.monolith.notification.dto.NotificationRequest;
import com.urbano.monolith.notification.dto.NotificationResponse;
import com.urbano.monolith.notification.dto.NotificationStatsDto;
import com.urbano.monolith.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ============================================================
    // SEND NOTIFICATION
    // ============================================================
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody NotificationRequest request) {
        request.setPmAccountId(pmAccountId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendNotification(request));
    }

    // ============================================================
    // GET NOTIFICATION BY ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotification(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(notificationService.getNotification(id, pmAccountId));
    }

    // ============================================================
    // GET USER NOTIFICATIONS
    // ============================================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<NotificationDto>> getUserNotifications(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pmAccountId, page, size));
    }

    // ============================================================
    // GET UNREAD NOTIFICATIONS
    // ============================================================
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, pmAccountId));
    }

    // ============================================================
    // GET UNREAD COUNT
    // ============================================================
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId, pmAccountId));
    }

    // ============================================================
    // MARK AS READ
    // ============================================================
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, pmAccountId));
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        notificationService.markAllAsRead(userId, pmAccountId);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // DELETE NOTIFICATION
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        notificationService.deleteNotification(id, pmAccountId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // GET NOTIFICATION STATS
    // ============================================================
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDto> getStats(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(notificationService.getStats(pmAccountId));
    }
}