package com.urbano.monolith.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatisticsDto {
    private long totalReports;
    private long completedReports;
    private long pendingReports;
    private long failedReports;
    private long expiredReports;
    private long totalFileSize;
    private int reportsByType;
    private int reportsByFormat;
}