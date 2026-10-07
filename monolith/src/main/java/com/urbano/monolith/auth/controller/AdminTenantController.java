package com.urbano.monolith.auth.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.InviteStatus;
import com.urbano.monolith.auth.dto.AdminInviteCodeDto;
import com.urbano.monolith.auth.dto.AdminTenantDto;
import com.urbano.monolith.auth.service.AdminTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * SUPER_ADMIN-only tenant oversight.
 *
 * <p>Locked down by {@code SecurityConfig}: all {@code /api/admin/**}
 * paths require {@code ROLE_SUPER_ADMIN}.</p>
 */
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final AdminTenantService service;

    @GetMapping
    public ResponseEntity<PagedResponse<AdminTenantDto>> list(
            @RequestParam(value = "status", required = false) InviteStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }

    /**
     * Support tool: fetch the live 6-digit invite code for a PENDING tenant.
     * Read-only — does NOT consume the code, so the tenant can still use it.
     */
    @GetMapping("/{id}/invite-code")
    public ResponseEntity<AdminInviteCodeDto> getInviteCode(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getInviteCode(id));
    }
}