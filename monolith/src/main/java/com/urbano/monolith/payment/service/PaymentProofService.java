package com.urbano.monolith.payment.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.payment.dto.PaymentProofDto;
import com.urbano.monolith.payment.dto.PaymentProofRequest;
import com.urbano.monolith.payment.dto.PaymentProofResolveRequest;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.entity.PaymentProof;
import com.urbano.monolith.payment.repository.PaymentProofRepository;
import com.urbano.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProofService {

    private final PaymentProofRepository paymentProofRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * PATCH 7: Submit a payment proof
     */
    @Transactional
    public PaymentProofDto submitPaymentProof(UUID tenantId, PaymentProofRequest request) {
        // Check if proof already exists for this receipt
        if (request.getMpesaReceiptNumber() != null) {
            List<PaymentProof> existing = paymentProofRepository
                    .findByMpesaReceiptNumber(request.getMpesaReceiptNumber());
            if (!existing.isEmpty()) {
                throw new RuntimeException("Proof already submitted for this receipt");
            }
        }

        // ✅ Create the proof builder first
        PaymentProof.PaymentProofBuilder builder = PaymentProof.builder()
                .pmAccountId(getPmAccountIdForTenant(tenantId))
                .tenantId(tenantId)
                .unitId(request.getUnitId())
                .mpesaReceiptNumber(request.getMpesaReceiptNumber())
                .screenshotUrl(request.getScreenshotUrl())
                .amountClaimed(request.getAmountClaimed())
                .submittedAt(LocalDateTime.now())
                .status(PaymentProof.PaymentProofStatus.PENDING_REVIEW);

        // ✅ Check for matching payment BEFORE building the proof
        PaymentProof proof;
        if (request.getMpesaReceiptNumber() != null) {
            String receiptNumber = request.getMpesaReceiptNumber();
            Optional<Payment> matchingPayment = paymentRepository.findByMpesaReceiptNumber(receiptNumber);

            if (matchingPayment.isPresent()) {
                Payment payment = matchingPayment.get();
                // ✅ Build the proof with matched status
                proof = builder
                        .paymentId(payment.getId())
                        .status(PaymentProof.PaymentProofStatus.MATCHED)
                        .resolvedAt(LocalDateTime.now())
                        .build();

                // Mark payment as reconciled if not already
                if (!payment.isReconciled()) {
                    paymentService.reconcilePayment(payment.getId());
                }
            } else {
                // ✅ No matching payment found
                proof = builder.build();
            }
        } else {
            // ✅ No receipt number provided
            proof = builder.build();
        }

        // Save the proof
        proof = paymentProofRepository.save(proof);
        log.info("Payment proof submitted: {} for tenant {}", proof.getId(), tenantId);
        return mapToDto(proof);
    }

    /**
     * PATCH 7: Get pending proofs for PM review
     */
    public PagedResponse<PaymentProofDto> getPendingProofs(UUID pmAccountId, int page, int size) {
        Page<PaymentProof> proofPage = paymentProofRepository
                .findByPmAccountIdAndStatus(
                        pmAccountId,
                        PaymentProof.PaymentProofStatus.PENDING_REVIEW,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "submittedAt"))
                );
        return mapToPagedResponse(proofPage);
    }

    /**
     * PATCH 7: Resolve a proof (match or reject)
     */
    @Transactional
    public PaymentProofDto resolveProof(UUID id, UUID pmAccountId, PaymentProofResolveRequest request) {
        PaymentProof proof = paymentProofRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment proof not found"));

        if (proof.getStatus() != PaymentProof.PaymentProofStatus.PENDING_REVIEW) {
            throw new RuntimeException("Proof is already resolved");
        }

        if ("MATCHED".equalsIgnoreCase(request.getResolution())) {
            if (request.getMatchedPaymentId() == null) {
                throw new RuntimeException("Payment ID required for matching");
            }

            Payment payment = paymentRepository.findById(request.getMatchedPaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            proof.setPaymentId(payment.getId());
            proof.setStatus(PaymentProof.PaymentProofStatus.MATCHED);

            // Reconcile the payment
            if (!payment.isReconciled()) {
                paymentService.reconcilePayment(payment.getId());
            }

            log.info("Payment proof {} matched to payment {}", id, payment.getId());

        } else if ("REJECTED".equalsIgnoreCase(request.getResolution())) {
            proof.setStatus(PaymentProof.PaymentProofStatus.REJECTED);
            log.info("Payment proof {} rejected", id);

        } else {
            throw new RuntimeException("Invalid resolution status. Use MATCHED or REJECTED");
        }

        proof.setResolvedAt(LocalDateTime.now());
        proof.setResolutionNotes(request.getNotes());

        proof = paymentProofRepository.save(proof);
        return mapToDto(proof);
    }

    /**
     * Get proof by ID with PM validation
     */
    public PaymentProofDto getProof(UUID id, UUID pmAccountId) {
        PaymentProof proof = paymentProofRepository.findByIdAndPmAccountId(id, pmAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment proof not found"));
        return mapToDto(proof);
    }

    /**
     * Get proofs for a tenant
     */
    public PagedResponse<PaymentProofDto> getTenantProofs(UUID tenantId, int page, int size) {
        // TODO: Add tenant-based query
        return PagedResponse.empty(PageRequest.of(page, size));
    }

    /**
     * Get PM account ID for tenant
     */
    private UUID getPmAccountIdForTenant(UUID tenantId) {
        // TODO: Call tenant-service to get pmAccountId
        return UUID.randomUUID();
    }

    /**
     * Map to DTO
     */
    private PaymentProofDto mapToDto(PaymentProof proof) {
        return PaymentProofDto.builder()
                .id(proof.getId())
                .pmAccountId(proof.getPmAccountId())
                .tenantId(proof.getTenantId())
                .unitId(proof.getUnitId())
                .paymentId(proof.getPaymentId())
                .mpesaReceiptNumber(proof.getMpesaReceiptNumber())
                .screenshotUrl(proof.getScreenshotUrl())
                .amountClaimed(proof.getAmountClaimed())
                .status(proof.getStatus())
                .submittedAt(proof.getSubmittedAt())
                .resolvedAt(proof.getResolvedAt())
                .resolvedBy(proof.getResolvedBy())
                .resolutionNotes(proof.getResolutionNotes())
                .createdAt(proof.getCreatedAt())
                .build();
    }

    /**
     * Map page to paged response
     */
    private PagedResponse<PaymentProofDto> mapToPagedResponse(Page<PaymentProof> page) {
        List<PaymentProofDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<PaymentProofDto>builder()
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