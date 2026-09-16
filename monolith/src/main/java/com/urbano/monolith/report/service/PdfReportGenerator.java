package com.urbano.monolith.report.service;

import com.urbano.monolith.report.dto.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfReportGenerator {

    public byte[] generate(ReportRequest request) {
        log.info("Generating PDF report: type={}", request.getType());

        // TODO: Implement actual PDF generation using JasperReports or iText
        // This is a placeholder that returns a sample PDF
        return createSamplePdf(request);
    }

    private byte[] createSamplePdf(ReportRequest request) {
        String content = """
                Urbano Homes Report
                ===================
                Type: %s
                Format: PDF
                Generated: %s
                Description: %s
                """.formatted(request.getType(), java.time.LocalDateTime.now(), request.getDescription());

        return content.getBytes();
    }
}