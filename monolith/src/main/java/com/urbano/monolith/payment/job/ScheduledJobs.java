package com.urbano.monolith.payment.job;

import com.urbano.monolith.payment.service.LateFeeService;
import com.urbano.monolith.payment.service.ManagementFeeService;
import com.urbano.monolith.payment.service.MpesaReconciliationService;
import com.urbano.monolith.payment.service.RentReminderService;
import com.urbano.monolith.tenant.service.LeaseExpiryService;
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
    private final MpesaReconciliationService mpesaReconciliationService;
    private final LeaseExpiryService leaseExpiryService;
    private final LateFeeService lateFeeService;

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
     * Reconcile pending payments - runs every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "reconcilePayments", lockAtMostFor = "5m", lockAtLeastFor = "10s")
    public void reconcilePendingPayments() {
        log.info("Starting payment reconciliation job");
        try {
            mpesaReconciliationService.reconcilePendingPayments();
        } catch (Exception e) {
            log.error("Error in payment reconciliation job: {}", e.getMessage(), e);
        }
        log.info("Payment reconciliation job completed");
    }

    /**
     * Daily lease expiration alerts job - runs at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = "leaseExpiryAlerts", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void checkLeaseExpiries() {
        log.info("Starting lease expiry notification job");
        try {
            leaseExpiryService.processUpcomingExpiries();
        } catch (Exception e) {
            log.error("Error in lease expiry notification job: {}", e.getMessage(), e);
        }
        log.info("Lease expiry notification job completed");
    }

    /**
     * Daily late fee assessment job - runs at 10 AM on the 6th of each month
     */
    @Scheduled(cron = "0 0 10 6 * *")
    @SchedulerLock(name = "lateFeeAssessment", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void assessLateFees() {
        log.info("Starting rent late fee assessment job");
        try {
            lateFeeService.processLateFees();
        } catch (Exception e) {
            log.error("Error in rent late fee assessment job: {}", e.getMessage(), e);
        }
        log.info("Rent late fee assessment job completed");
    }
}