package com.urbano.monolith.report.service;

import com.urbano.monolith.report.dto.ReportRequest;
import com.urbano.monolith.report.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerator {

    // ✅ Now depends on ReportPersistenceService instead of ReportService
    private final ReportPersistenceService reportPersistenceService;
    private final PdfReportGenerator pdfReportGenerator;
    private final ExcelReportGenerator excelReportGenerator;
    private final CsvReportGenerator csvReportGenerator;

    /**
     * Generate a report based on type and format
     */
    @Transactional
    public Report generateReport(ReportRequest request) {
        log.info("Generating report: type={}, format={}", request.getType(), request.getFormat());

        // Create report record
        Report report = Report.builder()
                .pmAccountId(request.getPmAccountId())
                .name(request.getName())
                .type(request.getType())
                .format(request.getFormat())
                .description(request.getDescription())
                .generatedBy(getCurrentUserId())
                .status("PROCESSING")
                .parameters(request.getAdditionalParameters() != null ?
                        request.getAdditionalParameters().toString() : null)
                .createdAt(LocalDateTime.now())
                .build();

        report = reportPersistenceService.save(report);

        try {
            // Generate report based on format
            byte[] reportData = generateReportData(request);

            // Save the report file
            String filePath = saveReportFile(report.getId(), reportData, request.getFormat());
            String fileName = generateFileName(report, request.getFormat());

            // Update report record
            report.setStatus("COMPLETED");
            report.setFilePath(filePath);
            report.setFileName(fileName);
            report.setFileSize((long) reportData.length);
            report.setCompletedAt(LocalDateTime.now());
            report.setExpiresAt(LocalDateTime.now().plusDays(30));

            report = reportPersistenceService.save(report);
            log.info("Report generated successfully: {}", report.getId());

        } catch (Exception e) {
            log.error("Failed to generate report: {}", e.getMessage());
            report.setStatus("FAILED");
            report.setErrorMessage(e.getMessage());
            report = reportPersistenceService.save(report);
            throw new RuntimeException("Report generation failed: " + e.getMessage());
        }

        return report;
    }

    /**
     * Generate report data based on type and format
     */
    private byte[] generateReportData(ReportRequest request) {
        String format = request.getFormat().toUpperCase();

        return switch (format) {
            case "PDF" -> pdfReportGenerator.generate(request);
            case "EXCEL" -> excelReportGenerator.generate(request);
            case "CSV" -> csvReportGenerator.generate(request);
            default -> throw new RuntimeException("Unsupported format: " + format);
        };
    }

    /**
     * Save report file to storage
     */
    private String saveReportFile(UUID reportId, byte[] data, String format) {
        // TODO: Implement file storage (local file system, S3, etc.)
        String filePath = "/reports/" + reportId + "." + format.toLowerCase();
        log.info("Report saved to: {}", filePath);
        return filePath;
    }

    /**
     * Generate file name
     */
    private String generateFileName(Report report, String format) {
        return report.getName().replaceAll("\\s+", "_") + "_" +
                report.getId().toString().substring(0, 8) + "." +
                format.toLowerCase();
    }

    /**
     * Get current user ID from security context
     */
    private UUID getCurrentUserId() {
        // TODO: Get from security context
        return UUID.randomUUID();
    }
}