package com.urbano.monolith.report.service;

import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.report.entity.Report;
import com.urbano.monolith.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportPersistenceService {

    private final ReportRepository reportRepository;

    @Transactional
    public Report save(Report report) {
        log.debug("Saving report: {}", report.getId());
        return reportRepository.save(report);
    }

    public Report findByIdAndPmAccountId(UUID id, UUID pmAccountId) {
        return reportRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    public boolean existsByIdAndPmAccountId(UUID id, UUID pmAccountId) {
        return reportRepository.findByIdAndPmAccountId(id, pmAccountId).isPresent();
    }

    @Transactional
    public void softDelete(Report report) {
        report.setDeletedAt(LocalDateTime.now());
        reportRepository.save(report);
        log.info("Report soft deleted: {}", report.getId());
    }

    @Transactional
    public void deleteByIdAndPmAccountId(UUID id, UUID pmAccountId) {
        Report report = findByIdAndPmAccountId(id, pmAccountId);
        softDelete(report);
    }
}