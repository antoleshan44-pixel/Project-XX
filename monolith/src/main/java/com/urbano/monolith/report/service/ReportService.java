package com.urbano.monolith.report.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.report.dto.ReportDto;
import com.urbano.monolith.report.dto.ReportRequest;
import com.urbano.monolith.report.dto.ReportStatisticsDto;
import com.urbano.monolith.report.entity.Report;
import com.urbano.monolith.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportGenerator reportGenerator;
    // ✅ Add ReportPersistenceService
    private final ReportPersistenceService reportPersistenceService;

    /**
     * Generate a report synchronously
     */
    @Transactional
    public ReportDto generateReport(ReportRequest request) {
        log.info("Generating report: type={}, format={}", request.getType(), request.getFormat());

        Report report = reportGenerator.generateReport(request);
        return mapToDto(report);
    }

    /**
     * Generate a report asynchronously
     */
    @Async
    @Transactional
    public void generateReportAsync(ReportRequest request) {
        try {
            generateReport(request);
        } catch (Exception e) {
            log.error("Async report generation failed: {}", e.getMessage());
        }
    }

    /**
     * Get report by ID - Scoped to PM Account
     */
    public ReportDto getReport(UUID id, UUID pmAccountId) {
        // ✅ Use ReportPersistenceService
        Report report = reportPersistenceService.findByIdAndPmAccountId(id, pmAccountId);
        return mapToDto(report);
    }

    /**
     * Get all reports for a PM account
     */
    public PagedResponse<ReportDto> getReports(UUID pmAccountId, int page, int size) {
        Page<Report> reports = reportRepository.findByPmAccountId(
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(reports);
    }

    /**
     * Get reports by type - Scoped to PM Account
     */
    public PagedResponse<ReportDto> getReportsByType(UUID pmAccountId, String type, int page, int size) {
        Page<Report> reports = reportRepository.findByPmAccountIdAndType(
                pmAccountId,
                type,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(reports);
    }

    /**
     * Get reports by status - Scoped to PM Account
     */
    public PagedResponse<ReportDto> getReportsByStatus(UUID pmAccountId, String status, int page, int size) {
        Page<Report> reports = reportRepository.findByPmAccountIdAndStatus(
                pmAccountId,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(reports);
    }

    /**
     * Get reports by user - Scoped to PM Account
     */
    public PagedResponse<ReportDto> getUserReports(UUID userId, UUID pmAccountId, int page, int size) {
        Page<Report> reports = reportRepository.findByGeneratedByAndPmAccountId(
                userId,
                pmAccountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapToPagedResponse(reports);
    }

    /**
     * Download report file
     */
    public byte[] downloadReport(UUID id, UUID pmAccountId) {
        // ✅ Use ReportPersistenceService
        Report report = reportPersistenceService.findByIdAndPmAccountId(id, pmAccountId);

        if (!report.isCompleted()) {
            throw new RuntimeException("Report is not ready for download");
        }

        // TODO: Read file from storage
        return ("Report content for " + report.getName()).getBytes();
    }

    /**
     * Delete report - Scoped to PM Account
     */
    @Transactional
    public void deleteReport(UUID id, UUID pmAccountId) {
        // ✅ Use ReportPersistenceService
        reportPersistenceService.deleteByIdAndPmAccountId(id, pmAccountId);
        log.info("Report deleted: {}", id);
    }

    /**
     * Get report statistics - Scoped to PM Account
     */
    public ReportStatisticsDto getStatistics(UUID pmAccountId) {
        long totalReports = reportRepository.countByPmAccountId(pmAccountId);
        long completedReports = reportRepository.countByPmAccountIdAndStatus(pmAccountId, "COMPLETED");
        long pendingReports = reportRepository.countByPmAccountIdAndStatus(pmAccountId, "PENDING");
        long failedReports = reportRepository.countByPmAccountIdAndStatus(pmAccountId, "FAILED");
        long expiredReports = reportRepository.countByPmAccountIdAndExpiresAtBefore(pmAccountId, LocalDateTime.now());

        return ReportStatisticsDto.builder()
                .totalReports(totalReports)
                .completedReports(completedReports)
                .pendingReports(pendingReports)
                .failedReports(failedReports)
                .expiredReports(expiredReports)
                .build();
    }

    /**
     * Save report (internal method) - Delegated to ReportPersistenceService
     */
    public Report save(Report report) {
        return reportPersistenceService.save(report);
    }

    // ============================================================
    // MAPPER METHODS
    // ============================================================
    private ReportDto mapToDto(Report report) {
        return ReportDto.builder()
                .id(report.getId())
                .pmAccountId(report.getPmAccountId())
                .name(report.getName())
                .type(report.getType())
                .format(report.getFormat())
                .description(report.getDescription())
                .status(report.getStatus())
                .filePath(report.getFilePath())
                .fileName(report.getFileName())
                .fileSize(report.getFileSize())
                .generatedBy(report.getGeneratedBy())
                .parameters(report.getParameters())
                .errorMessage(report.getErrorMessage())
                .createdAt(report.getCreatedAt())
                .completedAt(report.getCompletedAt())
                .expiresAt(report.getExpiresAt())
                .build();
    }

    private PagedResponse<ReportDto> mapToPagedResponse(Page<Report> page) {
        List<ReportDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<ReportDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}