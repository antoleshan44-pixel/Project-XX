package com.urbano.monolith.report.dto;

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
public class ReportDto {
    private UUID id;
    private UUID pmAccountId;
    private String name;
    private String type;
    private String format;
    private String description;
    private String status;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private UUID generatedBy;
    private String generatedByName;
    private String parameters;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}