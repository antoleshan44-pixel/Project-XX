package com.urbano.monolith.report.service;

import com.urbano.monolith.report.dto.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CsvReportGenerator {

    public byte[] generate(ReportRequest request) {
        log.info("Generating CSV report: type={}", request.getType());

        // TODO: Implement actual CSV generation
        return createSampleCsv(request);
    }

    private byte[] createSampleCsv(ReportRequest request) {
        String content = """
                Report Type,Format,Generated
                %s,%s,%s
                """.formatted(request.getType(), "CSV", java.time.LocalDateTime.now());

        return content.getBytes();
    }
}