package com.urbano.monolith.dashboard.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.LeaseStatus;
import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.dashboard.dto.DashboardSummaryResponse;
import com.urbano.monolith.dashboard.dto.PropertyOccupancyDto;
import com.urbano.monolith.maintenance.repository.MaintenanceRepository;
import com.urbano.monolith.payment.repository.PaymentRepository;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.tenant.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardSummaryService {

    private static final Set<PaymentStatus> REVENUE_STATUSES =
            EnumSet.of(PaymentStatus.COMPLETED, PaymentStatus.RECONCILED, PaymentStatus.PAID);

    private static final Set<MaintenanceStatus> OPEN_MAINTENANCE_STATUSES =
            EnumSet.of(MaintenanceStatus.SUBMITTED, MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.OPEN);

    private static final int REVENUE_WINDOW_MONTHS = 12;

    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("PM account context required");
        }

        long totalProperties = propertyRepository.countByPmAccountId(pmAccountId);
        long activeLeases = leaseRepository
                .countByPmAccountIdAndIsActiveTrueAndStatus(pmAccountId, LeaseStatus.ACTIVE);
        long openMaintenance = maintenanceRepository
                .countByPmAccountIdAndStatusIn(pmAccountId, OPEN_MAINTENANCE_STATUSES);

        YearMonth currentMonth = YearMonth.now();
        YearMonth windowStart = currentMonth.minusMonths(REVENUE_WINDOW_MONTHS - 1);
        LocalDateTime from = windowStart.atDay(1).atStartOfDay();

        List<Object[]> paymentRows = paymentRepository.findPaymentsSince(
                pmAccountId, REVENUE_STATUSES, from);

        BigDecimal[] monthly = new BigDecimal[REVENUE_WINDOW_MONTHS];
        for (int i = 0; i < REVENUE_WINDOW_MONTHS; i++) monthly[i] = BigDecimal.ZERO;

        LocalDateTime startOfCurrentMonth = currentMonth.atDay(1).atStartOfDay();
        BigDecimal currentMonthRevenue = BigDecimal.ZERO;

        // Month index: year * 12 + month, both 0-based-agnostic (relative
        // differences are all that matter, so offset by 1 is fine).
        long currentIdx = (long) currentMonth.getYear() * 12 + currentMonth.getMonthValue();

        for (Object[] row : paymentRows) {
            LocalDateTime ts = (LocalDateTime) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (ts == null || amount == null) continue;

            YearMonth ym = YearMonth.from(ts);
            long ymIdx = (long) ym.getYear() * 12 + ym.getMonthValue();
            long monthsAgo = currentIdx - ymIdx;

            if (monthsAgo >= 0 && monthsAgo < REVENUE_WINDOW_MONTHS) {
                int slot = REVENUE_WINDOW_MONTHS - 1 - (int) monthsAgo;
                monthly[slot] = monthly[slot].add(amount);
            }

            if (!ts.isBefore(startOfCurrentMonth)) {
                currentMonthRevenue = currentMonthRevenue.add(amount);
            }
        }

        List<Object[]> occRows = propertyRepository.findOccupancyByProperty(pmAccountId);
        List<PropertyOccupancyDto> occupancy = new ArrayList<>(occRows.size());
        for (Object[] row : occRows) {
            UUID propertyId = (UUID) row[0];
            String name = (String) row[1];
            long occupied = toLong(row[2]);
            long total = toLong(row[3]);
            occupancy.add(PropertyOccupancyDto.builder()
                    .propertyId(propertyId)
                    .name(name)
                    .occupiedUnits(occupied)
                    .totalUnits(total)
                    .build());
        }

        log.info("Dashboard summary for PM {}: props={}, activeLeases={}, openMaint={}, monthRev={}",
                pmAccountId, totalProperties, activeLeases, openMaintenance, currentMonthRevenue);

        List<BigDecimal> revenueByMonth = new ArrayList<>(REVENUE_WINDOW_MONTHS);
        for (BigDecimal m : monthly) revenueByMonth.add(m);

        return DashboardSummaryResponse.builder()
                .totalProperties(totalProperties)
                .activeLeases(activeLeases)
                .openMaintenance(openMaintenance)
                .monthlyRevenue(currentMonthRevenue)
                .revenueByMonth(revenueByMonth)
                .occupancyByProperty(occupancy)
                .build();
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }
}