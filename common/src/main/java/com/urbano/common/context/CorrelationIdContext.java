package com.urbano.common.context;

import org.slf4j.MDC;

import java.util.UUID;

public class CorrelationIdContext {

    private static final String CORRELATION_ID_KEY = "correlationId";

    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}