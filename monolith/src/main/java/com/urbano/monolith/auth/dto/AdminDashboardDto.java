package com.urbano.monolith.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {

    private long totalPmAccounts;
    private long pendingPmAccounts;
    private long activePmAccounts;
    private long suspendedPmAccounts;

    private long totalProperties;
    private long pendingProperties;
    private long approvedProperties;
    private long rejectedProperties;
    private long suspendedProperties;
}
