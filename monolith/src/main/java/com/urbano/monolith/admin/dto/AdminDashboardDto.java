package com.urbano.monolith.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private long pmAccountsTotal;
    private long pmAccountsActive;
    private long pmAccountsSuspended;
    private long propertiesTotal;
    private long propertiesPendingApproval;
    private long propertiesApproved;
    private long propertiesRejected;
    private long propertiesSuspended;
    private long tenantsTotal;
    private long usersTotal;
}