package com.urbano.monolith.auth.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.monolith.auth.dto.AdminPropertyActionRequest;
import com.urbano.monolith.auth.service.AdminPropertyService;
import com.urbano.monolith.property.dto.PropertyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/properties")
@RequiredArgsConstructor
public class AdminPropertyController {

    private final AdminPropertyService service;

    @GetMapping
    public ResponseEntity<PagedResponse<PropertyDto>> list(
            @RequestParam(value = "status", required = false) PropertyApprovalStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.listByApprovalStatus(status, pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PropertyDto> approve(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPropertyActionRequest req) {
        return ResponseEntity.ok(service.approve(id, req.getReason()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PropertyDto> reject(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPropertyActionRequest req) {
        return ResponseEntity.ok(service.reject(id, req.getReason()));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<PropertyDto> suspend(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPropertyActionRequest req) {
        return ResponseEntity.ok(service.suspend(id, req.getReason()));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<PropertyDto> reactivate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPropertyActionRequest req) {
        return ResponseEntity.ok(service.reactivate(id, req.getReason()));
    }
}
