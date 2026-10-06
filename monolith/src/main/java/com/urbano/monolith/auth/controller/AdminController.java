package com.urbano.monolith.auth.controller;

import com.urbano.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    /**
     * Phase 2A probe: verifies SUPER_ADMIN authorization + TenantContext.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("role", TenantContext.getUserRole());
        body.put("userId", TenantContext.getUserId());
        body.put("pmAccountId", TenantContext.getPmAccountId());
        body.put("adminBypass", TenantContext.isAdminBypass());
        return ResponseEntity.ok(body);
    }
}
