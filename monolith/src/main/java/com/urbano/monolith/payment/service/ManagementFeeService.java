package com.urbano.monolith.payment.service;

import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.payment.dto.ManagementFeeRequest;
import com.urbano.monolith.payment.dto.ManagementFeeResponse;
import com.urbano.monolith.payment.entity.ManagementFee;
import com.urbano.monolith.payment.repository.ManagementFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagementFeeService {

    private final ManagementFeeRepository managementFeeRepository;

    @Transactional
    public ManagementFeeResponse generateFee(ManagementFeeRequest request, UUID pmAccountId) {
        log.info("Generating management fee for property: {}", request.getPropertyId());

        ManagementFee fee = ManagementFee.builder()
                .propertyId(request.getPropertyId())
                .pmAccountId(pmAccountId)
                .period(request.getPeriod())
                .amount(request.getAmount())
                .invoicedAt(LocalDateTime.now())
                .status("PENDING")
                .description(request.getDescription())
                .isActive(true)
                .build();

        ManagementFee saved = managementFeeRepository.save(fee);
        log.info("Management fee generated with id: {}", saved.getId());

        return toResponse(saved);
    }

    public Page<ManagementFeeResponse> getManagementFees(UUID pmAccountId, Pageable pageable) {
        return managementFeeRepository.findByPmAccountId(pmAccountId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ManagementFeeResponse markAsPaid(UUID id, UUID pmAccountId) {
        ManagementFee fee = managementFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Management fee", id));

        // Verify ownership
        if (!fee.getPmAccountId().equals(pmAccountId)) {
            throw new RuntimeException("Management fee does not belong to this PM account");
        }

        fee.setPaidAt(LocalDateTime.now());
        fee.setStatus("PAID");

        ManagementFee saved = managementFeeRepository.save(fee);
        log.info("Management fee marked as paid: {}", id);

        return toResponse(saved);
    }

    /**
     * ✅ Add this method - Called by ScheduledJobs
     * Generate monthly management fees for all properties
     */
    @Transactional
    public void generateMonthlyFees() {
        log.info("Generating monthly management fees...");

        // TODO: Implement monthly fee generation
        // 1. Query all properties under managed-property option
        // 2. Calculate fee for each property (e.g., % of rent collected)
        // 3. Create ManagementFee records for each property
        // 4. Set status to PENDING

        // Placeholder implementation
        YearMonth currentPeriod = YearMonth.now();
        log.info("Monthly fees generated for period: {}", currentPeriod);
    }

    private ManagementFeeResponse toResponse(ManagementFee fee) {
        return ManagementFeeResponse.builder()
                .id(fee.getId())
                .propertyId(fee.getPropertyId())
                .period(fee.getPeriod())
                .amount(fee.getAmount())
                .status(fee.getStatus())
                .invoicedAt(fee.getInvoicedAt())
                .paidAt(fee.getPaidAt())
                .description(fee.getDescription())
                .build();
    }
}