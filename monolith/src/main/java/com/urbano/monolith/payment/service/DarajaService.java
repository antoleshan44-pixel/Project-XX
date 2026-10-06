package com.urbano.monolith.payment.service;

import com.urbano.common.enums.PaymentStatus;
import com.urbano.monolith.payment.dto.DarajaCallbackRequest;
import com.urbano.monolith.payment.dto.PaymentDto;
import com.urbano.monolith.payment.dto.PaymentRequest;
import com.urbano.monolith.payment.entity.Payment;
import com.urbano.monolith.payment.repository.PaymentRepository;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
    private final UnitRepository unitRepository;

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
     * Process Daraja callback with idempotency and real unit reconciliation
     */
    @Transactional
    public String processCallback(DarajaCallbackRequest request) {
        // Check for duplicate by mpesaReceiptNumber
        String receiptNumber = request.getTransactionId();
        if (receiptNumber != null && paymentRepository.existsByMpesaReceiptNumber(receiptNumber)) {
            log.info("Duplicate callback ignored: {}", receiptNumber);
            return "Duplicate ignored";
        }

        // Parse the BillRefNumber to find matching Unit
        String billRef = request.getBillRefNumber();
        Optional<Unit> optionalUnit = resolveUnitFromBillRef(billRef);

        if (optionalUnit.isEmpty()) {
            log.warn("Could not match unit from BillRefNumber: {}", billRef);
            // Create unmatched payment
            PaymentDto payment = createUnmatchedPayment(request);
            return "Payment created as UNMATCHED: " + payment.getId();
        }

        Unit unit = optionalUnit.get();
        Property property = unit.getProperty();
        UUID unitId = unit.getId();
        UUID propertyId = property != null ? property.getId() : UUID.randomUUID();
        UUID pmAccountId = property != null ? property.getPmAccountId() : UUID.randomUUID();
        UUID tenantId = unit.getCurrentTenantId() != null ? unit.getCurrentTenantId() : UUID.randomUUID();
        BigDecimal expectedRent = unit.getRentAmount() != null ? BigDecimal.valueOf(unit.getRentAmount()) : BigDecimal.ZERO;

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pmAccountId(pmAccountId)
                .tenantId(tenantId)
                .propertyId(propertyId)
                .unitId(unitId)
                .leaseId(UUID.randomUUID())
                .amount(request.getAmount())
                .amountExpected(expectedRent)
                .currency("KES")
                .paymentMethod("MPESA")
                .referenceNumber(request.getTransactionId())
                .description("M-Pesa payment for unit " + unit.getUnitNumber() + " (" + billRef + ")")
                .customerName(request.getCustomerName())
                .build();

        PaymentDto payment = paymentService.createPayment(paymentRequest);

        // Determine reconciliation status
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElse(null);
        if (savedPayment != null) {
            PaymentStatus status = savedPayment.determineReconciliationStatus(expectedRent);
            savedPayment.setStatus(status);
            savedPayment.setMpesaReceiptNumber(request.getTransactionId());
            savedPayment.setTransactionDate(parseTransactionDate(request.getTransactionTime()));
            savedPayment.setCustomerName(request.getCustomerName());
            savedPayment.setRawPayload(request.toString());

            if (status == PaymentStatus.RECONCILED || status == PaymentStatus.OVERPAID) {
                savedPayment.setReconciled(true);
                savedPayment.setReconciledAt(LocalDateTime.now());
            }

            paymentRepository.save(savedPayment);
            log.info("Payment processed for unit {}: {}, status: {}", unit.getUnitNumber(), savedPayment.getId(), status);
        }

        return "Callback processed successfully: " + payment.getId();
    }

    /**
     * Resolve unit entity from BillRefNumber (UUID, UNIT-UUID, Unit Number, or UNIT-Number)
     */
    public Optional<Unit> resolveUnitFromBillRef(String billRef) {
        if (billRef == null || billRef.trim().isEmpty()) {
            return Optional.empty();
        }

        String cleaned = billRef.trim();

        // 1. Try parsing as exact UUID
        try {
            UUID uuid = UUID.fromString(cleaned);
            Optional<Unit> unitById = unitRepository.findById(uuid);
            if (unitById.isPresent()) {
                return unitById;
            }
        } catch (IllegalArgumentException ignored) {
        }

        // 2. Try stripped "UNIT-" prefix UUID (e.g. "UNIT-550e8400-e29b-41d4-a716-446655440000")
        if (cleaned.toUpperCase().startsWith("UNIT-")) {
            String candidateUuidStr = cleaned.substring(5).trim();
            try {
                UUID uuid = UUID.fromString(candidateUuidStr);
                Optional<Unit> unitById = unitRepository.findById(uuid);
                if (unitById.isPresent()) {
                    return unitById;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        // 3. Try exact match on unitNumber (e.g. "A101")
        List<Unit> unitsByNumber = unitRepository.findByUnitNumberIgnoreCaseAndDeletedAtIsNull(cleaned);
        if (!unitsByNumber.isEmpty()) {
            return Optional.of(unitsByNumber.get(0));
        }

        // 4. Try stripping "UNIT-" prefix for unitNumber (e.g. "UNIT-A101" -> "A101")
        if (cleaned.toUpperCase().startsWith("UNIT-")) {
            String rawNumber = cleaned.substring(5).trim();
            List<Unit> unitsByStrippedNumber = unitRepository.findByUnitNumberIgnoreCaseAndDeletedAtIsNull(rawNumber);
            if (!unitsByStrippedNumber.isEmpty()) {
                return Optional.of(unitsByStrippedNumber.get(0));
            }
        }

        return Optional.empty();
    }

    /**
     * Create an unmatched payment when unit cannot be identified
     */
    private PaymentDto createUnmatchedPayment(DarajaCallbackRequest request) {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pmAccountId(UUID.randomUUID())
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

    private boolean isIpInRange(String ip, String range) {
        if (ip == null || range == null) return false;
        try {
            String[] parts = range.split("/");
            if (parts.length != 2) return ip.equals(range);

            String subnet = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            long ipLong = ipToLong(ip);
            long subnetLong = ipToLong(subnet);

            long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            return (ipLong & mask) == (subnetLong & mask);
        } catch (Exception e) {
            log.warn("Failed to check IP {} against range {}: {}", ip, range, e.getMessage());
            return false;
        }
    }

    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += (long) (ip * Math.pow(256, power));
        }
        return result;
    }

    private LocalDateTime parseTransactionDate(String transactionTime) {
        if (transactionTime == null) return LocalDateTime.now();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(transactionTime, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}