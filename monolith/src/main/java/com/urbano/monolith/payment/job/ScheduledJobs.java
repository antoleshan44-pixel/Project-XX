package com.urbano.monolith.payment.job;

import com.urbano.monolith.payment.service.ManagementFeeService;  // ✅ Add this import
import com.urbano.monolith.payment.service.RentReminderService;   // ✅ Add this import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    private final RentReminderService rentReminderService;
    private final ManagementFeeService managementFeeService;

    /**
     * Daily rent reminder job - runs at 8 AM
     */
    @Scheduled(cron = "0 0 8 * * *")
    @SchedulerLock(name = "rentReminders", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void sendRentReminders() {
        log.info("Starting rent reminder job");
        try {
            rentReminderService.sendReminders();
        } catch (Exception e) {
            log.error("Error in rent reminder job: {}", e.getMessage(), e);
        }
        log.info("Rent reminder job completed");
    }

    /**
     * Monthly management fee generation - runs on the 1st at 2 AM
     */
    @Scheduled(cron = "0 0 2 1 * *")
    @SchedulerLock(name = "managementFees", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void generateManagementFees() {
        log.info("Starting management fee generation job");
        try {
            managementFeeService.generateMonthlyFees();
        } catch (Exception e) {
            log.error("Error in management fee generation job: {}", e.getMessage(), e);
        }
        log.info("Management fee generation job completed");
    }

    /**
     * Reconcile pending payments - runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "reconcilePayments", lockAtMostFor = "1m")
    public void reconcilePendingPayments() {
        log.info("Starting payment reconciliation job");
        try {
            // TODO: Implement pending payment reconciliation
        } catch (Exception e) {
            log.error("Error in payment reconciliation job: {}", e.getMessage(), e);
        }
        log.info("Payment reconciliation job completed");
    }
}