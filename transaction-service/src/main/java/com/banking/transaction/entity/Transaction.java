package com.banking.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", schema = "transaction_schema",
        indexes = {
                @Index(name = "idx_txn_source", columnList = "source_account_id"),
                @Index(name = "idx_txn_dest", columnList = "destination_account_id"),
                @Index(name = "idx_txn_reference", columnList = "reference_number", unique = true),
                @Index(name = "idx_txn_created", columnList = "created_at"),
                @Index(name = "idx_txn_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 30)
    private String referenceNumber;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "destination_currency", length = 3)
    private String destinationCurrency;

    @Column(name = "exchange_rate", precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "converted_amount", precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", length = 20)
    @Builder.Default
    private FraudStatus fraudStatus = FraudStatus.PENDING;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "reversed_transaction_id")
    private UUID reversedTransactionId;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransactionType {
        TRANSFER, UPI, REFUND, REVERSAL
    }

    public enum TransactionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REVERSED
    }

    public enum FraudStatus {
        PENDING, CLEARED, FLAGGED, BLOCKED
    }
}
