package com.urbano.monolith.auth.repository;

import com.urbano.monolith.auth.entity.PmAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PmAccountRepository extends JpaRepository<PmAccount, UUID> {
    Optional<PmAccount> findByCompanyName(String companyName);
    boolean existsByCompanyName(String companyName);
}