package com.urbano.monolith.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDto {
    private long totalSent;
    private long totalDelivered;
    private long totalRead;
    private long totalFailed;
    private long pendingCount;
    private long unreadCount;
}