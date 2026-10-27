package com.urbano.monolith.auth.security;

import com.urbano.monolith.auth.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts requests to sensitive endpoints and enforces rate limits.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = extractClientIp(request);

        boolean allowed = true;
        int retryAfter = 60;

        if ("POST".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/auth/login")) {
                allowed = rateLimiterService.tryAcquire("login", clientIp, 10, 60);
                retryAfter = 60;
            } else if (path.startsWith("/api/auth/register")) {
                allowed = rateLimiterService.tryAcquire("register", clientIp, 5, 300);
                retryAfter = 120;
            } else if (path.startsWith("/api/auth/password/forgot")) {
                allowed = rateLimiterService.tryAcquire("password_forgot", clientIp, 5, 900);
                retryAfter = 300;
            } else if (path.startsWith("/api/auth/password/reset")) {
                allowed = rateLimiterService.tryAcquire("password_reset", clientIp, 5, 900);
                retryAfter = 300;
            } else if (path.startsWith("/api/public/contact")) {
                allowed = rateLimiterService.tryAcquire("public_contact", clientIp, 10, 300);
                retryAfter = 60;
            } else if (path.startsWith("/api/public/newsletter/subscribe")) {
                allowed = rateLimiterService.tryAcquire("newsletter_subscribe", clientIp, 10, 300);
                retryAfter = 60;
            }
        }

        if (!allowed) {
            log.warn("Rate limit exceeded for IP {} on {} {}", clientIp, method, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
