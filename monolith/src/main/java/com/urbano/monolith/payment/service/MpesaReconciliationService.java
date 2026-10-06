package com.urbano.monolith.payment.service;

import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaReconciliationService {

    private final PaymentRepository paymentRepository;

    /**
     * Polling job to reconcile pending or un-reconciled M-Pesa payments
     */
    @Transactional
    public void reconcilePendingPayments() {
        log.info("Running M-Pesa STK Push payment reconciliation & timeout polling...");

        List<Payment> unreconciled = paymentRepository.findByIsReconciledFalse();
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(2);

        int reconciledCount = 0;
        int timedOutCount = 0;

        for (Payment payment : unreconciled) {
            // Check if payment has been pending for over 2 minutes without M-Pesa callback
            if (payment.getCreatedAt() != null && payment.getCreatedAt().isBefore(timeoutThreshold)) {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    log.info("M-Pesa payment {} timed out waiting for callback — marking FAILED", payment.getId());
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setReconciled(true);
                    payment.setReconciledAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    timedOutCount++;
                } else if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.RECONCILED) {
                    payment.setReconciled(true);
                    payment.setReconciledAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    reconciledCount++;
                }
            }
        }

        log.info("M-Pesa reconciliation completed: {} payments marked reconciled, {} marked failed.",
                reconciledCount, timedOutCount);
    }
}
