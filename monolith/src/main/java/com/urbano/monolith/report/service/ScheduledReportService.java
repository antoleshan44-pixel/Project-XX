package com.urbano.monolith.report.service;

import com.urbano.monolith.report.constants.ReportFormat;
import com.urbano.monolith.report.constants.ReportType;
import com.urbano.monolith.report.dto.ReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportService {

    // ✅ Use ReportGenerator directly (no circular dependency now)
    private final ReportGenerator reportGenerator;

    /**
     * Generate daily financial report at 6 AM
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void generateDailyFinancialReport() {
        log.info("Generating daily financial report");
        try {
            ReportRequest request = ReportRequest.builder()
                    .pmAccountId(getSystemPmAccountId())
                    .name("Daily Financial Report - " + LocalDate.now())
                    .type(ReportType.FINANCIAL_REPORT)
                    .format(ReportFormat.PDF)
                    .description("Daily financial summary")
                    .startDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now())
                    .build();
            reportGenerator.generateReport(request);
            log.info("Daily financial report generated successfully");
        } catch (Exception e) {
            log.error("Failed to generate daily financial report: {}", e.getMessage());
        }
    }

    /**
     * Generate monthly tenant report on the 1st at 2 AM
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void generateMonthlyTenantReport() {
        log.info("Generating monthly tenant report");
        try {
            ReportRequest request = ReportRequest.builder()
                    .pmAccountId(getSystemPmAccountId())
                    .name("Monthly Tenant Report - " + LocalDate.now().getMonth())
                    .type(ReportType.TENANT_REPORT)
                    .format(ReportFormat.EXCEL)
                    .description("Monthly tenant summary")
                    .startDate(LocalDate.now().withDayOfMonth(1))
                    .endDate(LocalDate.now())
                    .build();
            reportGenerator.generateReport(request);
            log.info("Monthly tenant report generated successfully");
        } catch (Exception e) {
            log.error("Failed to generate monthly tenant report: {}", e.getMessage());
        }
    }

    /**
     * Generate weekly payment report every Monday at 8 AM
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklyPaymentReport() {
        log.info("Generating weekly payment report");
        try {
            ReportRequest request = ReportRequest.builder()
                    .pmAccountId(getSystemPmAccountId())
                    .name("Weekly Payment Report - Week " + LocalDate.now().getDayOfYear() / 7)
                    .type(ReportType.PAYMENT_REPORT)
                    .format(ReportFormat.CSV)
                    .description("Weekly payment summary")
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .build();
            reportGenerator.generateReport(request);
            log.info("Weekly payment report generated successfully");
        } catch (Exception e) {
            log.error("Failed to generate weekly payment report: {}", e.getMessage());
        }
    }

    /**
     * Clean up expired reports daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanExpiredReports() {
        log.info("Cleaning expired reports");
        // TODO: Implement report cleanup
    }

    private UUID getSystemPmAccountId() {
        // TODO: Get system PM account ID
        return UUID.randomUUID();
    }
}