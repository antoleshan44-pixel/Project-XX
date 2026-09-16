package com.urbano.monolith.payment.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.enums.PaymentStatus;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.PaymentRequest;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentDto createPayment(PaymentRequest request) {
        Payment payment = Payment.builder()
                .pmAccountId(request.getPmAccountId())
                .tenantId(request.getTenantId())
                .propertyId(request.getPropertyId())
                .unitId(request.getUnitId())
                .leaseId(request.getLeaseId())
                .amount(request.getAmount())
                .amountExpected(request.getAmountExpected())
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "MPESA")
                .referenceNumber(request.getReferenceNumber() != null ?
                        request.getReferenceNumber() : UUID.randomUUID().toString().substring(0, 12))
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .customerName(request.getCustomerName())
                .transactionId(UUID.randomUUID().toString())
                .paymentGateway("MPESA")
                .isReconciled(false)
                .createdAt(LocalDateTime.now())
                .build();

        // M-Pesa specific fields
        if ("MPESA".equalsIgnoreCase(request.getPaymentMethod())) {
            payment.setMpesaReceiptNumber("MP" + System.currentTimeMillis());
            payment.setTransactionDate(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);
        log.info("Payment created: {}", payment.getId());
        return mapToDto(payment);
    }

    public PaymentDto getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapToDto(payment);
    }

    public PagedResponse<PaymentDto> getPaymentsByTenant(UUID tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return mapToPagedResponse(paymentPage);
    }

    public PagedResponse<PaymentDto> getPaymentsByProperty(UUID propertyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId, pageable);
        return mapToPagedResponse(paymentPage);
    }

    public PagedResponse<PaymentDto> getPaymentsByStatus(PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findByStatus(status, pageable);
        return mapToPagedResponse(paymentPage);
    }

    public PagedResponse<PaymentDto> getPaymentsByPmAccount(UUID pmAccountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findByPmAccountIdOrderByTransactionDateDesc(pmAccountId, pageable);
        return mapToPagedResponse(paymentPage);
    }

    public PagedResponse<PaymentDto> getPaymentsByPmAccountAndStatus(UUID pmAccountId, PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findByPmAccountIdAndStatusOrderByTransactionDateDesc(pmAccountId, status, pageable);
        return mapToPagedResponse(paymentPage);
    }

    public PagedResponse<PaymentDto> getPaymentsByPmAccountAndTenant(UUID pmAccountId, UUID tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findByPmAccountIdAndTenantIdOrderByTransactionDateDesc(pmAccountId, tenantId, pageable);
        return mapToPagedResponse(paymentPage);
    }

    @Transactional
    public PaymentDto updatePaymentStatus(UUID id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setStatus(status);

        if (status == PaymentStatus.PAID) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);
        log.info("Payment status updated: {} -> {}", id, status);
        return mapToDto(payment);
    }

    @Transactional
    public PaymentDto reconcilePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // ✅ Determine reconciliation status based on expected amount
        // NOTE: DB CHECK constraint only allows: PENDING, PAID, FAILED, REFUNDED, CANCELLED, COMPLETED
        // We use COMPLETED for successful reconciliation
        if (payment.getAmountExpected() != null) {
            BigDecimal amount = payment.getAmount();
            BigDecimal expected = payment.getAmountExpected();
            int comparison = amount.compareTo(expected);
            if (comparison == 0) {
                payment.setStatus(PaymentStatus.COMPLETED);
            } else if (comparison > 0) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setAmountDifference(amount.subtract(expected));
            } else {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setAmountDifference(expected.subtract(amount));
            }
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
        }

        payment.setReconciled(true);
        payment.setReconciledAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        log.info("Payment reconciled: {} with status {}", id, payment.getStatus());
        return mapToDto(payment);
    }

    @Transactional
    public PaymentDto unreconcilePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setReconciled(false);
        payment.setReconciledAt(null);
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);
        log.info("Payment unreconciled: {}", id);
        return mapToDto(payment);
    }

    public List<PaymentDto> getUnreconciledPayments() {
        return paymentRepository.findByIsReconciledFalse().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PaymentDto> getPaymentsByMpesaReceipt(String receiptNumber) {
        return paymentRepository.findByMpesaReceiptNumber(receiptNumber)
                .map(List::of)
                .orElse(List.of())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public boolean existsByMpesaReceiptNumber(String receiptNumber) {
        return paymentRepository.existsByMpesaReceiptNumber(receiptNumber);
    }

    // ============================================================
    // MAPPER METHODS
    // ============================================================
    public PaymentDto mapToDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .pmAccountId(payment.getPmAccountId())
                .tenantId(payment.getTenantId())
                .propertyId(payment.getPropertyId())
                .unitId(payment.getUnitId())
                .leaseId(payment.getLeaseId())
                .amount(payment.getAmount())
                .amountExpected(payment.getAmountExpected())
                .amountDifference(payment.getAmountDifference())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .dueDate(payment.getDueDate())
                .description(payment.getDescription())
                .isReconciled(payment.isReconciled())
                .reconciledAt(payment.getReconciledAt())
                .mpesaReceiptNumber(payment.getMpesaReceiptNumber())
                .transactionDate(payment.getTransactionDate())
                .customerName(payment.getCustomerName())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private PagedResponse<PaymentDto> mapToPagedResponse(Page<Payment> page) {
        List<PaymentDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<PaymentDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}