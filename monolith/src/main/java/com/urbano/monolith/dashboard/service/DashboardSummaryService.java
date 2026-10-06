package com.urbano.monolith.dashboard.service;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.LeaseStatus;
import com.urbano.common.enums.MaintenanceStatus;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.common.enums.UnitStatus;
import com.urbano.common.exception.UnauthorizedException;
import com.urbano.monolith.dashboard.dto.DashboardSummaryResponse;
import com.urbano.monolith.dashboard.dto.PropertyOccupancyDto;
import com.urbano.monolith.maintenance.repository.MaintenanceRepository;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.property.repository.UnitRepository;
import com.urbano.monolith.tenant.entity.Lease;
import com.urbano.monolith.tenant.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardSummaryService {

    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentRepository paymentRepository;
    private final UnitRepository unitRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        UUID pmAccountId = TenantContext.getPmAccountId();
        if (pmAccountId == null) {
            throw new UnauthorizedException("PM account context required");
        }

        log.info("Fetching dashboard summary for PM account: {}", pmAccountId);

        // 1. Total Properties
        List<Property> properties = propertyRepository.findByPmAccountId(pmAccountId, Pageable.unpaged()).getContent();
        long totalProperties = properties.size();

        // 2. Active Leases
        List<Lease> leases = leaseRepository.findByPmAccountId(pmAccountId, Pageable.unpaged()).getContent();
        long activeLeases = leases.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()) && l.getStatus() == LeaseStatus.ACTIVE)
                .count();

        // 3. Open Maintenance Requests
        long openMaintenance = maintenanceRepository.findByPmAccountId(pmAccountId, Pageable.unpaged()).getContent().stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.SUBMITTED 
                          || m.getStatus() == MaintenanceStatus.IN_PROGRESS 
                          || m.getStatus() == MaintenanceStatus.OPEN)
                .count();

        // 4. Monthly Revenue (Current Month & Past 12 Months)
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentYearMonth = YearMonth.from(now);
        LocalDateTime startOfCurrentMonth = currentYearMonth.atDay(1).atStartOfDay();

        List<Payment> pmPayments = paymentRepository.findByPmAccountIdOrderByTransactionDateDesc(pmAccountId, Pageable.unpaged()).getContent();

        BigDecimal monthlyRevenue = pmPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED || p.getStatus() == PaymentStatus.RECONCILED || p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getTransactionDate() != null && !p.getTransactionDate().isBefore(startOfCurrentMonth))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue array for past 12 months (oldest to current)
        List<BigDecimal> revenueByMonth = new ArrayList<>(12);
        for (int i = 11; i >= 0; i--) {
            YearMonth targetMonth = currentYearMonth.minusMonths(i);
            LocalDateTime monthStart = targetMonth.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = targetMonth.atEndOfMonth().atTime(23, 59, 59);

            BigDecimal monthTotal = pmPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.COMPLETED || p.getStatus() == PaymentStatus.RECONCILED || p.getStatus() == PaymentStatus.PAID)
                    .filter(p -> p.getTransactionDate() != null 
                              && !p.getTransactionDate().isBefore(monthStart) 
                              && !p.getTransactionDate().isAfter(monthEnd))
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueByMonth.add(monthTotal);
        }

        // 5. Occupancy By Property
        List<PropertyOccupancyDto> occupancyByProperty = new ArrayList<>();
        for (Property property : properties) {
            List<Unit> units = unitRepository.findByPropertyId(property.getId(), Pageable.unpaged()).getContent();
            long totalUnits = property.getTotalUnits() != null ? property.getTotalUnits() : units.size();
            long occupiedUnits = units.stream()
                    .filter(u -> u.getStatus() == UnitStatus.OCCUPIED || u.getStatus() == UnitStatus.RENTED || Boolean.FALSE.equals(u.getIsAvailable()))
                    .count();

            occupancyByProperty.add(PropertyOccupancyDto.builder()
                    .propertyId(property.getId())
                    .name(property.getName())
                    .occupiedUnits(occupiedUnits)
                    .totalUnits(totalUnits)
                    .build());
        }

        return DashboardSummaryResponse.builder()
                .totalProperties(totalProperties)
                .activeLeases(activeLeases)
                .openMaintenance(openMaintenance)
                .monthlyRevenue(monthlyRevenue)
                .revenueByMonth(revenueByMonth)
                .occupancyByProperty(occupancyByProperty)
                .build();
    }
}
