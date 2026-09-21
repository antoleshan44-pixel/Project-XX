package com.urbano.monolith.property.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public payload for POST /api/units/{unitId}/viewings.
 *
 * <p>No auth required — a prospective renter browsing listings is not
 * necessarily logged in. If the requester <em>is</em> authenticated, the mobile
 * app should pass {@code requestedByUserId} so their viewings can be listed
 * later via a user-scoped endpoint.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewingRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255)
    private String requestedByName;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15)
    private String requestedByPhone;

    /** Optional; only set when the requester is logged in. */
    private UUID requestedByUserId;

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Scheduled date/time must be in the future")
    private LocalDateTime scheduledAt;

    /** Optional PM-facing notes. Not shown publicly. */
    private String notes;
}