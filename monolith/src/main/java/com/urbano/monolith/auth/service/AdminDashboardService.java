package com.urbano.monolith.auth.service;

import com.urbano.common.enums.PmAccountStatus;
import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.monolith.auth.dto.AdminDashboardDto;
import com.urbano.monolith.auth.repository.PmAccountRepository;
import com.urbano.monolith.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final PmAccountRepository pmAccountRepository;
    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto summary() {
        long totalPm = pmAccountRepository.count();
        long activePm = pmAccountRepository.countByApprovalStatus(PmAccountStatus.ACTIVE);
        long suspendedPm = pmAccountRepository.countByApprovalStatus(PmAccountStatus.SUSPENDED);

        long totalProp = propertyRepository.count();
        long pendingProp = propertyRepository.countByApprovalStatus(PropertyApprovalStatus.PENDING_APPROVAL);
        long approvedProp = propertyRepository.countByApprovalStatus(PropertyApprovalStatus.APPROVED);
        long rejectedProp = propertyRepository.countByApprovalStatus(PropertyApprovalStatus.REJECTED);
        long suspendedProp = propertyRepository.countByApprovalStatus(PropertyApprovalStatus.SUSPENDED);

        return AdminDashboardDto.builder()
                .totalPmAccounts(totalPm)
                .pendingPmAccounts(0)
                .activePmAccounts(activePm)
                .suspendedPmAccounts(suspendedPm)
                .totalProperties(totalProp)
                .pendingProperties(pendingProp)
                .approvedProperties(approvedProp)
                .rejectedProperties(rejectedProp)
                .suspendedProperties(suspendedProp)
                .build();
    }
}
