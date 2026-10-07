package com.urbano.monolith.dashboard.controller;

import com.urbano.monolith.dashboard.dto.ActivityItemDto;
import com.urbano.monolith.dashboard.dto.DashboardSummaryResponse;
import com.urbano.monolith.dashboard.service.DashboardActivityService;
import com.urbano.monolith.dashboard.service.DashboardSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardSummaryService dashboardSummaryService;
    private final DashboardActivityService dashboardActivityService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('PM_ADMIN','PM_STAFF')")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardSummaryService.getDashboardSummary());
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('PM_ADMIN','PM_STAFF')")
    public ResponseEntity<List<ActivityItemDto>> getRecentActivity(
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return ResponseEntity.ok(dashboardActivityService.getRecentActivity(limit));
    }
}