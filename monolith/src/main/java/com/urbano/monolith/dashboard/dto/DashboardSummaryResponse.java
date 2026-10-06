package com.urbano.monolith.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalProperties;
    private long activeLeases;
    private long openMaintenance;
    private BigDecimal monthlyRevenue;
    private List<BigDecimal> revenueByMonth;
    private List<PropertyOccupancyDto> occupancyByProperty;
}
