package com.urbano.monolith.crm.repository;

import com.urbano.monolith.crm.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Optional<Contact> findByEmail(String email);

    Optional<Contact> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // ✅ Scoped queries by PM Account
    Page<Contact> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<Contact> findByPmAccountIdAndIsActiveTrue(UUID pmAccountId, Pageable pageable);

    Page<Contact> findByPmAccountIdAndType(UUID pmAccountId, String type, Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.pmAccountId = :pmAccountId AND c.isActive = :active")
    Page<Contact> findByPmAccountIdAndIsActive(@Param("pmAccountId") UUID pmAccountId,
                                               @Param("active") boolean active,
                                               Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.pmAccountId = :pmAccountId AND " +
            "(:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contact> searchContacts(@Param("pmAccountId") UUID pmAccountId,
                                 @Param("search") String search,
                                 Pageable pageable);

    // Legacy queries (for backward compatibility - will be removed)
    @Deprecated
    Page<Contact> findByType(String type, Pageable pageable);

    @Deprecated
    Page<Contact> findByIsActiveTrue(Pageable pageable);
}