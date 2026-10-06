package com.urbano.monolith.admin.service;

import com.urbano.common.enums.PmAccountStatus;
import com.urbano.common.enums.PropertyApprovalStatus;
import com.urbano.monolith.admin.dto.AdminDashboardDto;
import com.urbano.monolith.auth.repository.PmAccountRepository;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final PmAccountRepository pmAccountRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto summary() {
        return AdminDashboardDto.builder()
                .pmAccountsTotal(pmAccountRepository.count())
                .pmAccountsActive(pmAccountRepository.countByApprovalStatus(PmAccountStatus.ACTIVE))
                .pmAccountsSuspended(pmAccountRepository.countByApprovalStatus(PmAccountStatus.SUSPENDED))
                .propertiesTotal(propertyRepository.count())
                .propertiesPendingApproval(propertyRepository.countByApprovalStatus(PropertyApprovalStatus.PENDING_APPROVAL))
                .propertiesApproved(propertyRepository.countByApprovalStatus(PropertyApprovalStatus.APPROVED))
                .propertiesRejected(propertyRepository.countByApprovalStatus(PropertyApprovalStatus.REJECTED))
                .propertiesSuspended(propertyRepository.countByApprovalStatus(PropertyApprovalStatus.SUSPENDED))
                .tenantsTotal(tenantRepository.count())
                .usersTotal(userRepository.count())
                .build();
    }
}