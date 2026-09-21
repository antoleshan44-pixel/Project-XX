package com.urbano.monolith.property.controller;

import com.urbano.common.context.TenantContext;
import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.ViewingStatus;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.property.dto.ViewingDto;
import com.urbano.monolith.property.dto.ViewingRequest;
import com.urbano.monolith.property.dto.ViewingStatusUpdateRequest;
import com.urbano.monolith.property.service.ViewingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ViewingController {

    private final ViewingService viewingService;

    private UUID requireTenant() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("No tenant context — authentication required");
        }
        return pmAccountId;
    }

    // ============================================================
    // PUBLIC — create a viewing for a unit
    // ============================================================
    /**
     * Public endpoint — no auth required. A prospective renter browsing
     * listings is not necessarily logged in.
     *
     * <p>If the requester <em>is</em> authenticated, the mobile app may pass
     * {@code requestedByUserId} so the viewing can later be listed via a
     * user-scoped endpoint.</p>
     */
    @PostMapping("/api/units/{unitId}/viewings")
    public ResponseEntity<ViewingDto> createViewing(
            @PathVariable("unitId") UUID unitId,
            @Valid @RequestBody ViewingRequest request) {
        return ResponseEntity.ok(viewingService.createViewing(unitId, request));
    }

    // ============================================================
    // PM-scoped
    // ============================================================
    @GetMapping("/api/viewings")
    public ResponseEntity<PagedResponse<ViewingDto>> getViewings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(viewingService.getViewings(requireTenant(), page, size));
    }

    @GetMapping("/api/viewings/{id}")
    public ResponseEntity<ViewingDto> getViewing(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(viewingService.getViewing(id, requireTenant()));
    }

    @GetMapping("/api/viewings/status/{status}")
    public ResponseEntity<PagedResponse<ViewingDto>> getViewingsByStatus(
            @PathVariable("status") ViewingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(viewingService.getViewingsByStatus(requireTenant(), status, page, size));
    }

    @GetMapping("/api/units/{unitId}/viewings")
    public ResponseEntity<PagedResponse<ViewingDto>> getViewingsByUnit(
            @PathVariable("unitId") UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(viewingService.getViewingsByUnit(unitId, requireTenant(), page, size));
    }

    @PutMapping("/api/viewings/{id}/status")
    public ResponseEntity<ViewingDto> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ViewingStatusUpdateRequest request) {
        return ResponseEntity.ok(viewingService.updateStatus(id, requireTenant(), request));
    }

    // ============================================================
    // User-scoped — for future My Bookings (mobile on Firestore today)
    // ============================================================
    @GetMapping("/api/users/{userId}/viewings")
    public ResponseEntity<PagedResponse<ViewingDto>> getViewingsForUser(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(viewingService.getViewingsForUser(userId, page, size));
    }
}