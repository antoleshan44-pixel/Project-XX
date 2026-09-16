package com.urbano.monolith.payment.repository;

import com.urbano.monolith.payment.entity.PaymentProof;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProofRepository extends JpaRepository<PaymentProof, UUID> {

    Page<PaymentProof> findByPmAccountId(UUID pmAccountId, Pageable pageable);

    Page<PaymentProof> findByPmAccountIdAndStatus(
            UUID pmAccountId, PaymentProof.PaymentProofStatus status, Pageable pageable);

    List<PaymentProof> findByStatus(PaymentProof.PaymentProofStatus status);

    Optional<PaymentProof> findByIdAndPmAccountId(UUID id, UUID pmAccountId);

    List<PaymentProof> findByMpesaReceiptNumber(String mpesaReceiptNumber);

    Optional<PaymentProof> findByPaymentId(UUID paymentId);
}