package com.urbano.monolith.payment.service;

import com.urbano.common.enums.LeaseStatus;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.notification.dto.NotificationRequest;
import com.urbano.monolith.notification.service.NotificationService;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import com.urbano.monolith.tenant.entity.Lease;
import com.urbano.monolith.tenant.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LateFeeService {

    private static final BigDecimal DEFAULT_LATE_FEE_PERCENTAGE = new BigDecimal("0.05"); // 5% late penalty
    private static final int GRACE_PERIOD_DAYS = 5;

    private final LeaseRepository leaseRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    /**
     * Calculates late rent fees for active leases with overdue payments
     */
    @Transactional
    public void processLateFees() {
        log.info("Running automated rent late fee assessment job...");

        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();

        // Run assessment if past grace period (e.g. past the 5th of the month)
        if (dayOfMonth <= GRACE_PERIOD_DAYS) {
            log.info("Within rent grace period (days 1-{}). Skipping late fee calculations.", GRACE_PERIOD_DAYS);
            return;
        }

        List<Lease> activeLeases = leaseRepository.findAll().stream()
                .filter(Lease::isActive)
                .filter(l -> l.getStatus() == LeaseStatus.ACTIVE)
                .toList();

        int lateFeesAssessed = 0;
        for (Lease lease : activeLeases) {
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            // Check if tenant has paid rent for current month
            List<Payment> currentMonthPayments = paymentRepository.findByTenantIdAndStatus(
                    lease.getTenant() != null ? lease.getTenant().getId() : null,
                    PaymentStatus.COMPLETED
            ).stream()
             .filter(p -> p.getTransactionDate() != null && p.getTransactionDate().isAfter(startOfMonth))
             .toList();

            if (currentMonthPayments.isEmpty() && lease.getRentAmount() != null) {
                BigDecimal rentAmount = lease.getRentAmount();
                BigDecimal lateFee = rentAmount.multiply(DEFAULT_LATE_FEE_PERCENTAGE);

                log.warn("Late rent detected for lease {}: rent = {}, assessed late fee = {}",
                        lease.getId(), rentAmount, lateFee);

                if (lease.getTenant() != null && lease.getTenant().getPhone() != null) {
                    try {
                        NotificationRequest req = NotificationRequest.builder()
                                .pmAccountId(lease.getPmAccountId())
                                .userId(lease.getTenant().getId())
                                .type("LATE_FEE_NOTICE")
                                .channel("SMS")
                                .recipient(lease.getTenant().getPhone())
                                .subject("Overdue Rent & Late Fee Notice")
                                .content(String.format("Notice: Your rent payment of %s %s is overdue. A 5%% late fee of %s %s has been applied.",
                                        lease.getCurrency(), rentAmount, lease.getCurrency(), lateFee))
                                .build();

                        notificationService.sendNotificationAsync(req);
                        lateFeesAssessed++;
                    } catch (Exception e) {
                        log.warn("Failed to dispatch late fee alert for lease {}: {}", lease.getId(), e.getMessage());
                    }
                }
            }
        }

        log.info("Late fee assessment completed. {} late fee notices issued.", lateFeesAssessed);
    }
}
