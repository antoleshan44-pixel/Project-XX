package com.urbano.monolith.payment.service;

import com.urbano.common.enums.PaymentStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.ReconciliationDto;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Transactional
    public PaymentDto matchPayment(UUID paymentId) {
        return paymentService.reconcilePayment(paymentId);
    }

    @Transactional
    public PaymentDto unmatchPayment(UUID paymentId) {
        return paymentService.unreconcilePayment(paymentId);
    }

    public List<ReconciliationDto> getUnreconciledPayments() {
        return paymentRepository.findByIsReconciledFalse().stream()
                .map(this::mapToReconciliationDto)
                .collect(Collectors.toList());
    }

    public List<ReconciliationDto> getReconciledPayments(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByReconciledAtBetween(startDate, endDate).stream()
                .map(this::mapToReconciliationDto)
                .collect(Collectors.toList());
    }

    private ReconciliationDto mapToReconciliationDto(Payment payment) {
        return ReconciliationDto.builder()
                .id(payment.getId())
                .referenceNumber(payment.getReferenceNumber())
                .amount(payment.getAmount().doubleValue()) 
                .currency(payment.getCurrency())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .isReconciled(payment.isReconciled())
                .reconciledAt(payment.getReconciledAt())
                .build();
    }
}