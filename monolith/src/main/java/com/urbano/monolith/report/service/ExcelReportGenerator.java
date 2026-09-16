package com.urbano.monolith.report.service;

import com.urbano.monolith.report.dto.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExcelReportGenerator {

    public byte[] generate(ReportRequest request) {
        log.info("Generating Excel report: type={}", request.getType());

        // TODO: Implement actual Excel generation using Apache POI
        return createSampleExcel(request);
    }

    private byte[] createSampleExcel(ReportRequest request) {
        String content = """
                Report Type: %s
                Format: Excel
                Generated: %s
                """.formatted(request.getType(), java.time.LocalDateTime.now());

        return content.getBytes();
    }
}