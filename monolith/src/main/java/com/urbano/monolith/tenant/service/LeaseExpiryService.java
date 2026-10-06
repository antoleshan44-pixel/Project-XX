package com.urbano.monolith.tenant.service;

import com.urbano.common.enums.LeaseStatus;
import com.urbano.monolith.notification.dto.NotificationRequest;
import com.urbano.monolith.notification.service.NotificationService;
import com.urbano.monolith.tenant.entity.Lease;
import com.urbano.monolith.tenant.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseExpiryService {

    private final LeaseRepository leaseRepository;
    private final NotificationService notificationService;

    /**
     * Checks active leases for upcoming expiry (30-day and 7-day windows)
     */
    @Transactional
    public void processUpcomingExpiries() {
        log.info("Running automated lease expiry check...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysFromNow = now.plusDays(30);

        List<Lease> activeLeases = leaseRepository.findAll().stream()
                .filter(Lease::isActive)
                .filter(l -> l.getStatus() == LeaseStatus.ACTIVE)
                .filter(l -> l.getEndDate() != null && l.getEndDate().isAfter(now) && l.getEndDate().isBefore(thirtyDaysFromNow))
                .toList();

        int alertsSent = 0;
        for (Lease lease : activeLeases) {
            long daysRemaining = java.time.Duration.between(now, lease.getEndDate()).toDays();
            if (lease.getTenant() != null && lease.getTenant().getPhone() != null) {
                try {
                    NotificationRequest req = NotificationRequest.builder()
                            .pmAccountId(lease.getPmAccountId())
                            .userId(lease.getTenant().getId())
                            .type("LEASE_EXPIRY_ALERT")
                            .channel("SMS")
                            .recipient(lease.getTenant().getPhone())
                            .subject("Lease Renewal Alert")
                            .content(String.format("Dear %s, your lease is set to expire in %d days. Please contact your property manager to renew your lease agreement.",
                                    lease.getTenant().getFirstName(), daysRemaining))
                            .build();

                    notificationService.sendNotificationAsync(req);
                    alertsSent++;
                } catch (Exception e) {
                    log.warn("Failed to dispatch lease expiry alert for lease {}: {}", lease.getId(), e.getMessage());
                }
            }
        }

        log.info("Lease expiry check completed. Dispatched {} alerts.", alertsSent);
    }
}
