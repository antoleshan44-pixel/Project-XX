package com.urbano.monolith.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketReplyDto {
    private UUID id;
    private UUID senderUserId;
    private String senderName;
    private String senderRole;
    private String message;
    private LocalDateTime createdAt;
}