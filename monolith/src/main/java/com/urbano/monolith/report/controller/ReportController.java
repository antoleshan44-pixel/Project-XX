package com.urbano.monolith.report.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.report.dto.ReportDto;
import com.urbano.monolith.report.dto.ReportRequest;
import com.urbano.monolith.report.dto.ReportResponse;
import com.urbano.monolith.report.dto.ReportStatisticsDto;
import com.urbano.monolith.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ============================================================
    // GENERATE REPORT
    // ============================================================
    @PostMapping
    public ResponseEntity<ReportResponse> generateReport(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @Valid @RequestBody ReportRequest request) {
        request.setPmAccountId(pmAccountId);
        ReportDto report = reportService.generateReport(request);

        return ResponseEntity.ok(ReportResponse.builder()
                .id(report.getId())
                .status(report.getStatus())
                .message("Report generated successfully")
                .downloadUrl("/api/reports/" + report.getId() + "/download")
                .completedAt(report.getCompletedAt())
                .build());
    }

    // ============================================================
    // GET REPORT BY ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> getReport(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(reportService.getReport(id, pmAccountId));
    }

    // ============================================================
    // GET ALL REPORTS
    // ============================================================
    @GetMapping
    public ResponseEntity<PagedResponse<ReportDto>> getReports(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReports(pmAccountId, page, size));
    }

    // ============================================================
    // GET REPORTS BY TYPE
    // ============================================================
    @GetMapping("/type/{type}")
    public ResponseEntity<PagedResponse<ReportDto>> getReportsByType(
            @PathVariable("type") String type,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReportsByType(pmAccountId, type, page, size));
    }

    // ============================================================
    // GET REPORTS BY STATUS
    // ============================================================
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<ReportDto>> getReportsByStatus(
            @PathVariable("status") String status,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReportsByStatus(pmAccountId, status, page, size));
    }

    // ============================================================
    // GET USER REPORTS
    // ============================================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<ReportDto>> getUserReports(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getUserReports(userId, pmAccountId, page, size));
    }

    // ============================================================
    // DOWNLOAD REPORT
    // ============================================================
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        byte[] reportData = reportService.downloadReport(id, pmAccountId);
        ReportDto report = reportService.getReport(id, pmAccountId);
        String filename = report.getFileName() != null ? report.getFileName() :
                report.getName() + "." + report.getFormat().toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(reportData);
    }

    // ============================================================
    // DELETE REPORT
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        reportService.deleteReport(id, pmAccountId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // GET REPORT STATISTICS
    // ============================================================
    @GetMapping("/stats")
    public ResponseEntity<ReportStatisticsDto> getStatistics(
            @RequestHeader("X-Pm-Account-Id") UUID pmAccountId) {
        return ResponseEntity.ok(reportService.getStatistics(pmAccountId));
    }
}