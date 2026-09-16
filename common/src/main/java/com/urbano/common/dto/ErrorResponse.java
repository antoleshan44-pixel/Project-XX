package com.urbano.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.urbano.common.config.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String correlationId;
    private String errorCode;
    private int statusCode;
    private String message;
    private String path;
    private OffsetDateTime timestamp;
    private List<ValidationError> validationErrors;
    private Map<String, Object> additionalDetails;

    public static ErrorResponse of(ErrorCode errorCode, int statusCode, String message, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .statusCode(statusCode)
                .message(errorCode.getMessage())
                .path(path)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, int statusCode, String message, String path, Map<String, Object> details) {
        ErrorResponse response = of(errorCode, statusCode, message, path);
        response.setAdditionalDetails(details);
        return response;
    }
}