package com.urbano.monolith.notification.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
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

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        request.setPmAccountId(requireTenant());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendNotification(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotification(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(notificationService.getNotification(id, requireTenant()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<NotificationDto>> getUserNotifications(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, requireTenant(), page, size));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, requireTenant()));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId, requireTenant()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(id, requireTenant()));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable("userId") UUID userId) {
        notificationService.markAllAsRead(userId, requireTenant());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") UUID id) {
        notificationService.deleteNotification(id, requireTenant());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDto> getStats() {
        return ResponseEntity.ok(notificationService.getStats(requireTenant()));
    }
}