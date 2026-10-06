package com.urbano.monolith.auth.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PmAccountStatus;
import com.urbano.monolith.auth.dto.AdminActionReasonRequest;
import com.urbano.monolith.auth.dto.AdminPmAccountDto;
import com.urbano.monolith.auth.service.AdminPmAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pm-accounts")
@RequiredArgsConstructor
public class AdminPmAccountController {

    private final AdminPmAccountService service;

    @GetMapping
    public ResponseEntity<PagedResponse<AdminPmAccountDto>> list(
            @RequestParam(value = "status", required = false) PmAccountStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<AdminPmAccountDto> suspend(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminActionReasonRequest req) {
        return ResponseEntity.ok(service.suspend(id, req.getReason()));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<AdminPmAccountDto> reactivate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminActionReasonRequest req) {
        return ResponseEntity.ok(service.reactivate(id, req.getReason()));
    }
}
