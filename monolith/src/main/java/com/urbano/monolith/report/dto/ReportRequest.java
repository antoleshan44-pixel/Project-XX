package com.urbano.monolith.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {


    private UUID pmAccountId;

    @NotBlank(message = "Report name is required")
    private String name;

    @NotBlank(message = "Report type is required")
    private String type;

    @NotBlank(message = "Report format is required")
    private String format;

    private String description;

    // Report parameters
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID propertyId;
    private UUID unitId;
    private UUID tenantId;
    private String status;
    private Map<String, Object> additionalParameters;
    
    @Builder.Default
    private boolean generateAsync = false;
}