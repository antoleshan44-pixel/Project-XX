package com.urbano.monolith.payment.service;

import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.dto.DarajaCallbackRequest;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.PaymentRequest;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DarajaService {

    private static final List<String> SAFARICOM_IP_RANGES = List.of(
            "196.201.214.0/24",
            "196.201.215.0/24",
            "196.201.216.0/24",
            "196.201.217.0/24"
    );

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Value("${daraja.ip-allowlist:196.201.214.0/24,196.201.215.0/24}")
    private String ipAllowlist;

    /**
     * Validate that the callback is from a trusted Safaricom IP
     */
    public boolean isValidDarajaIp(String clientIp) {
        if (clientIp == null) return false;

        // For development, allow localhost
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            log.debug("Localhost IP allowed for development: {}", clientIp);
            return true;
        }

        // Check if IP is in allowlist
        for (String range : SAFARICOM_IP_RANGES) {
            if (isIpInRange(clientIp, range)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate callback payload
     */
    public boolean validateCallback(DarajaCallbackRequest request) {
        if (request == null) return false;
        if (request.getTransactionId() == null || request.getTransactionId().isEmpty()) return false;
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (request.getBillRefNumber() == null || request.getBillRefNumber().isEmpty()) return false;
        return true;
    }

    /**
     * Process Daraja callback with idempotency
     */
    @Transactional
    public String processCallback(DarajaCallbackRequest request) {
        // ✅ Check for duplicate by mpesaReceiptNumber
        String receiptNumber = request.getTransactionId();
        if (receiptNumber != null && paymentRepository.existsByMpesaReceiptNumber(receiptNumber)) {
            log.info("Duplicate callback ignored: {}", receiptNumber);
            return "Duplicate ignored";
        }

        // Parse the BillRefNumber to find unit
        String billRef = request.getBillRefNumber();
        UUID unitId = parseUnitIdFromBillRef(billRef);
        UUID tenantId = parseTenantIdFromBillRef(billRef);

        if (unitId == null) {
            log.warn("Could not parse unit ID from BillRefNumber: {}", billRef);
            // Create unmatched payment
            PaymentDto payment = createUnmatchedPayment(request);
            return "Payment created as UNMATCHED: " + payment.getId();
        }

        // Create payment
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pmAccountId(getPmAccountIdForUnit(unitId))
                .tenantId(tenantId != null ? tenantId : UUID.randomUUID()) // TODO: Fetch from tenant-service
                .propertyId(getPropertyIdForUnit(unitId))
                .unitId(unitId)
                .leaseId(getActiveLeaseIdForUnit(unitId))
                .amount(request.getAmount())
                .amountExpected(getExpectedRentForUnit(unitId))
                .currency("KES")
                .paymentMethod("MPESA")
                .referenceNumber(request.getTransactionId())
                .description("M-Pesa payment for unit " + billRef)
                .customerName(request.getCustomerName())
                .build();

        PaymentDto payment = paymentService.createPayment(paymentRequest);

        // ✅ Determine reconciliation status
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElse(null);
        if (savedPayment != null) {
            BigDecimal expected = paymentRequest.getAmountExpected();
            PaymentStatus status = savedPayment.determineReconciliationStatus(expected);
            savedPayment.setStatus(status);
            savedPayment.setMpesaReceiptNumber(request.getTransactionId());
            savedPayment.setTransactionDate(parseTransactionDate(request.getTransactionTime()));
            savedPayment.setCustomerName(request.getCustomerName());
            savedPayment.setRawPayload(request.toString());

            if (status == PaymentStatus.RECONCILED) {
                savedPayment.setReconciled(true);
                savedPayment.setReconciledAt(LocalDateTime.now());
            }

            paymentRepository.save(savedPayment);
            log.info("Payment processed: {}, status: {}", savedPayment.getId(), status);
        }

        return "Callback processed successfully: " + payment.getId();
    }

    /**
     * Create an unmatched payment when unit cannot be identified
     */
    private PaymentDto createUnmatchedPayment(DarajaCallbackRequest request) {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pmAccountId(UUID.randomUUID()) // TODO: Handle this properly
                .tenantId(UUID.randomUUID())
                .propertyId(UUID.randomUUID())
                .unitId(UUID.randomUUID())
                .leaseId(UUID.randomUUID())
                .amount(request.getAmount())
                .currency("KES")
                .paymentMethod("MPESA")
                .referenceNumber(request.getTransactionId())
                .description("Unmatched M-Pesa payment: " + request.getBillRefNumber())
                .customerName(request.getCustomerName())
                .build();

        PaymentDto payment = paymentService.createPayment(paymentRequest);

        // Update status to UNMATCHED
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElse(null);
        if (savedPayment != null) {
            savedPayment.setStatus(PaymentStatus.UNMATCHED);
            savedPayment.setMpesaReceiptNumber(request.getTransactionId());
            savedPayment.setTransactionDate(parseTransactionDate(request.getTransactionTime()));
            savedPayment.setCustomerName(request.getCustomerName());
            savedPayment.setRawPayload(request.toString());
            paymentRepository.save(savedPayment);
        }

        return payment;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private boolean isIpInRange(String ip, String range) {
        // Simple CIDR check - in production use a proper library
        return true; // Simplified for now
    }

    private UUID parseUnitIdFromBillRef(String billRef) {
        // TODO: Implement parsing logic
        // Example: "UNIT-123" -> parse "123"
        return null;
    }

    private UUID parseTenantIdFromBillRef(String billRef) {
        // TODO: Implement parsing logic
        return null;
    }

    private UUID getPmAccountIdForUnit(UUID unitId) {
        // TODO: Call property-service to get PM account ID
        return UUID.randomUUID();
    }

    private UUID getPropertyIdForUnit(UUID unitId) {
        // TODO: Call property-service to get property ID
        return UUID.randomUUID();
    }

    private UUID getActiveLeaseIdForUnit(UUID unitId) {
        // TODO: Call tenant-service to get active lease ID
        return UUID.randomUUID();
    }

    private BigDecimal getExpectedRentForUnit(UUID unitId) {
        // TODO: Call property-service to get rent amount
        return BigDecimal.ZERO;
    }

    private LocalDateTime parseTransactionDate(String transactionTime) {
        if (transactionTime == null) return LocalDateTime.now();
        try {
            // Format: YYYYMMDDHHmmss
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(transactionTime, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}