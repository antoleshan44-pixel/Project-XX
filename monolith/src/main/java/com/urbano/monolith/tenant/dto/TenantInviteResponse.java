package com.urbano.monolith.tenant.dto;

import com.urbano.common.enums.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for POST /api/tenants/invite and POST /api/tenants/{id}/resend-invite.
 * Never includes the invite code — that only goes over SMS/email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInviteResponse {
    private UUID tenantId;
    private UUID pmAccountId;
    private String fullName;
    private String email;
    private String phone;
    private UUID unitId;
    private InviteStatus inviteStatus;
    private LocalDateTime invitedAt;

    /**
     * True if an SMS was dispatched successfully. False if SMS failed — in which
     * case the tenant row still exists (invite_status=PENDING) and the PM can
     * retry via resend-invite. Never silent-fails: either the SMS is sent or
     * this flag is false.
     */
    private boolean smsDispatched;

    private String message;
}