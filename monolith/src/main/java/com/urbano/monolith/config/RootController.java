package com.urbano.monolith.config;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serves a small welcome payload at {@code GET /} so the base URL of the
 * deployed service returns something meaningful instead of a 401 (Spring
 * Security default for unauthenticated requests to a non-permitAll path).
 *
 * <p>This is a landing page for humans who paste the base URL into a browser,
 * not a functional endpoint. All API calls go through {@code /api/**}.</p>
 */
@RestController
public class RootController {

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> root() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", "urbano-homes-api");
        payload.put("status", "ok");
        payload.put("description", "Urbano Homes backend — property management API");
        payload.put("docs", "/swagger-ui.html");
        payload.put("openapi", "/v3/api-docs");
        payload.put("health", "/actuator/health");
        payload.put("auth", "/api/auth/login");
        return payload;
    }
}