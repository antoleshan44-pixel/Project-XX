package com.urbano.common.filter;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String PM_ACCOUNT_ID_HEADER = "X-Pm-Account-Id";
    private static final String ADMIN_BYPASS_HEADER = "X-Admin-Access";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userId = request.getHeader(USER_ID_HEADER);
            String userRole = request.getHeader(USER_ROLE_HEADER);
            String pmAccountId = request.getHeader(PM_ACCOUNT_ID_HEADER);
            String adminBypass = request.getHeader(ADMIN_BYPASS_HEADER);

            if (userId != null && !userId.isEmpty()) {
                TenantContext.setUserId(UUID.fromString(userId));
            }

            if (userRole != null && !userRole.isEmpty()) {
                TenantContext.setUserRole(UserRole.valueOf(userRole));
            }

            if (pmAccountId != null && !pmAccountId.isEmpty()) {
                TenantContext.setPmAccountId(UUID.fromString(pmAccountId));
            }

            if ("true".equalsIgnoreCase(adminBypass)) {
                TenantContext.setAdminBypass(true);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
