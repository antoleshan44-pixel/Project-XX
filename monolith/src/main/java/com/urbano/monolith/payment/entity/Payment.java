package com.urbano.monolith.payment.entity;

import com.urbano.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pm_account_id", nullable = false)
    private UUID pmAccountId;  // ✅ Added for scoping

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "lease_id", nullable = false)
    private UUID leaseId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_expected")
    private BigDecimal amountExpected;  // ✅ Added for partial/overpayment detection

    @Column(name = "amount_difference")
    private BigDecimal amountDifference;  // ✅ Added

    private String currency;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "reference_number", nullable = false)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    private String description;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "is_reconciled", nullable = false)
    @Builder.Default
    private boolean isReconciled = false;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "mpesa_receipt_number")
    private String mpesaReceiptNumber;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "customer_name")
    private String customerName;  // ✅ Added

    @Column(name = "raw_payload")
    @Lob
    private String rawPayload;  // ✅ Added for audit

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ✅ Helper method to determine reconciliation status
    public PaymentStatus determineReconciliationStatus(BigDecimal expectedAmount) {
        if (expectedAmount == null) {
            return PaymentStatus.RECONCILED;
        }

        this.amountExpected = expectedAmount;
        this.amountDifference = this.amount.subtract(expectedAmount);

        int comparison = this.amount.compareTo(expectedAmount);
        if (comparison == 0) {
            return PaymentStatus.RECONCILED;
        } else if (comparison > 0) {
            return PaymentStatus.OVERPAID;
        } else {
            return PaymentStatus.PARTIAL;
        }
    }
}