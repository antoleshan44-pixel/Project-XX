package com.urbano.monolith.report.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.UnauthorizedException;
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

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> generateReport(@Valid @RequestBody ReportRequest request) {
        request.setPmAccountId(requireTenant());
        ReportDto report = reportService.generateReport(request);

        return ResponseEntity.ok(ReportResponse.builder()
                .id(report.getId())
                .status(report.getStatus())
                .message("Report generated successfully")
                .downloadUrl("/api/reports/" + report.getId() + "/download")
                .completedAt(report.getCompletedAt())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> getReport(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(reportService.getReport(id, requireTenant()));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ReportDto>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReports(requireTenant(), page, size));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<PagedResponse<ReportDto>> getReportsByType(
            @PathVariable("type") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReportsByType(requireTenant(), type, page, size));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<ReportDto>> getReportsByStatus(
            @PathVariable("status") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReportsByStatus(requireTenant(), status, page, size));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<ReportDto>> getUserReports(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getUserReports(userId, requireTenant(), page, size));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable("id") UUID id) {
        UUID pmAccountId = requireTenant();
        byte[] reportData = reportService.downloadReport(id, pmAccountId);
        ReportDto report = reportService.getReport(id, pmAccountId);
        String filename = report.getFileName() != null ? report.getFileName() :
                report.getName() + "." + report.getFormat().toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(reportData);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable("id") UUID id) {
        reportService.deleteReport(id, requireTenant());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<ReportStatisticsDto> getStatistics() {
        return ResponseEntity.ok(reportService.getStatistics(requireTenant()));
    }
}