package com.urbano.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // Upstream SMS provider failed — 502 distinguishes this from our own 500s
    @ExceptionHandler(SmsDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleSmsDelivery(SmsDeliveryException ex) {
        log.error("SMS delivery failed", ex);
        return build(HttpStatus.BAD_GATEWAY, "SMS Delivery Failed",
                "Could not send SMS at this time. Please try again.");
    }

    // Email/SMS/Push channel delivery failed inside NotificationService
    @ExceptionHandler(NotificationDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationDelivery(NotificationDeliveryException ex) {
        log.error("Notification delivery failed", ex);
        return build(HttpStatus.BAD_GATEWAY, "Notification Delivery Failed",
                "Could not deliver notification at this time. Please try again.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        Map<String, Object> error = baseBody(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields are invalid");
        error.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Jackson could not deserialize the request body — malformed JSON, invalid
     * enum value, wrong field type, etc.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Rejected malformed request body: {}", detail);
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Request body could not be parsed: " + detail);
    }

    /**
     * A query or path parameter could not be converted to the expected type —
     * e.g. {@code ?transactionType=NONSENSE} (not a valid enum value),
     * {@code ?minPrice=abc} (not a number).
     *
     * <p>Covers {@link MethodArgumentTypeMismatchException} and any other
     * {@link TypeMismatchException} raised by Spring's argument resolvers.
     * Without this handler, the catch-all intercepts them and returns 500.</p>
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(Exception ex) {
        String paramName = null;
        String value = null;
        String expectedType = null;

        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            paramName = mismatch.getName();
            value = String.valueOf(mismatch.getValue());
            expectedType = mismatch.getRequiredType() != null
                    ? mismatch.getRequiredType().getSimpleName()
                    : "unknown";
        }

        String message = paramName != null
                ? String.format("Parameter '%s' has invalid value '%s'. Expected type: %s",
                paramName, value, expectedType)
                : "Request parameter could not be converted to the expected type";

        log.warn("Type mismatch: {}", message);
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    /**
     * A DB constraint was violated — NOT NULL, unique, foreign key, check.
     * Almost always a client-data problem, not a server problem.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Data integrity violation: {}", detail);
        return build(HttpStatus.CONFLICT, "Conflict",
                "The request conflicts with existing data or a database constraint");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(baseBody(status, error, message));
    }

    private Map<String, Object> baseBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}