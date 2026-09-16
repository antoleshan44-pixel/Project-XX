package com.urbano.monolith.payment.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.PaymentSummaryDto;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * PATCH 8: Get payment summary across all accounts
     */
    public PaymentSummaryDto getPaymentSummary(LocalDateTime startDate, LocalDateTime endDate) {
        // Get all payments with filters
        List<Payment> payments;
        if (startDate != null && endDate != null) {
            payments = paymentRepository.findByReconciledAtBetween(startDate, endDate);
        } else {
            payments = paymentRepository.findAll();
        }

        // Count by status
        long reconciled = payments.stream().filter(p -> p.getStatus() == PaymentStatus.RECONCILED).count();
        long partial = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PARTIAL).count();
        long overpaid = payments.stream().filter(p -> p.getStatus() == PaymentStatus.OVERPAID).count();
        long unmatched = payments.stream().filter(p -> p.getStatus() == PaymentStatus.UNMATCHED).count();
        long pendingReview = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING_REVIEW).count();

        // Sum amounts by status
        BigDecimal totalAmount = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reconciledAmount = sumByStatus(payments, PaymentStatus.RECONCILED);
        BigDecimal partialAmount = sumByStatus(payments, PaymentStatus.PARTIAL);
        BigDecimal overpaidAmount = sumByStatus(payments, PaymentStatus.OVERPAID);
        BigDecimal unmatchedAmount = sumByStatus(payments, PaymentStatus.UNMATCHED);

        return PaymentSummaryDto.builder()
                .totalPayments(payments.size())
                .reconciledCount(reconciled)
                .partialCount(partial)
                .overpaidCount(overpaid)
                .unmatchedCount(unmatched)
                .pendingReviewCount(pendingReview)
                .totalAmount(totalAmount)
                .reconciledAmount(reconciledAmount)
                .partialAmount(partialAmount)
                .overpaidAmount(overpaidAmount)
                .unmatchedAmount(unmatchedAmount)
                .build();
    }

    /**
     * PATCH 8: Get payments with filters
     */
    public PagedResponse<PaymentDto> getPayments(
            UUID pmAccountId,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        Page<Payment> paymentPage;
        if (pmAccountId != null) {
            paymentPage = paymentRepository.findByPmAccountIdWithFilters(pmAccountId, status, startDate, endDate, pageable);
        } else {
            // All accounts
            if (status != null) {
                paymentPage = paymentRepository.findByStatus(status, pageable);
            } else {
                paymentPage = paymentRepository.findAll(pageable);
            }
        }

        List<PaymentDto> content = paymentPage.getContent().stream()
                .map(paymentService::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<PaymentDto>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .build();
    }

    /**
     * Sum amounts by status
     */
    private BigDecimal sumByStatus(List<Payment> payments, PaymentStatus status) {
        return payments.stream()
                .filter(p -> p.getStatus() == status)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}