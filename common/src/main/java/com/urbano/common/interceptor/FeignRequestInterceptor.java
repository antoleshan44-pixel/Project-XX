package com.urbano.common.interceptor;

import com.urbano.common.context.CorrelationIdContext;
import com.urbano.common.context.TenantContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String PM_ACCOUNT_ID_HEADER = "X-Pm-Account-Id";
    private static final String ADMIN_BYPASS_HEADER = "X-Admin-Access";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = CorrelationIdContext.getCorrelationId();
        if (correlationId != null) {
            template.header(CORRELATION_ID_HEADER, correlationId);
        }

        if (TenantContext.getUserId() != null) {
            template.header(USER_ID_HEADER, TenantContext.getUserId().toString());
        }
        if (TenantContext.getUserRole() != null) {
            template.header(USER_ROLE_HEADER, TenantContext.getUserRole().name());
        }
        if (TenantContext.getPmAccountId() != null) {
            template.header(PM_ACCOUNT_ID_HEADER, TenantContext.getPmAccountId().toString());
        }
        if (TenantContext.isAdminBypass()) {
            template.header(ADMIN_BYPASS_HEADER, "true");
        }

        log.debug("Feign request to {} with correlation ID: {}", template.url(), correlationId);
    }
}