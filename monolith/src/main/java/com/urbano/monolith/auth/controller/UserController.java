package com.urbano.monolith.auth.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.auth.dto.StaffInviteRequest;
import com.urbano.monolith.auth.dto.StaffUserResponse;
import com.urbano.monolith.auth.dto.UpdateUserStatusRequest;
import com.urbano.monolith.auth.service.StaffUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final StaffUserService staffUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PM_ADMIN','PM_STAFF')")
    public ResponseEntity<PagedResponse<StaffUserResponse>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StaffUserResponse> result = staffUserService.listStaff(page, size);
        return ResponseEntity.ok(PagedResponse.<StaffUserResponse>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('PM_ADMIN')")
    public ResponseEntity<StaffUserResponse> inviteStaff(
            @Valid @RequestBody StaffInviteRequest request) {
        return ResponseEntity.ok(staffUserService.inviteStaff(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PM_ADMIN')")
    public ResponseEntity<StaffUserResponse> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(staffUserService.updateStatus(id, request));
    }
}