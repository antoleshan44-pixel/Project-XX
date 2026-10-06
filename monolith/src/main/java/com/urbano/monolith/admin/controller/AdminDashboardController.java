package com.urbano.monolith.admin.controller;

import com.urbano.monolith.admin.dto.AdminDashboardDto;
import com.urbano.monolith.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;

    @GetMapping
    public ResponseEntity<AdminDashboardDto> summary() {
        return ResponseEntity.ok(service.summary());
    }
}