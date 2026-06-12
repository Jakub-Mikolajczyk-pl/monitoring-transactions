package pl.jakubmikolajczyk.monitoring.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import pl.jakubmikolajczyk.monitoring.common.id.Uuids;

/// An immutable financial fact (ADR-0005): every column is insert-only and the class
/// exposes no mutators. Corrections happen through compensating entries, never edits.
///
/// The customer is referenced by id instead of a JPA association: Customer and
/// Transaction are separate consistency boundaries, and the schema-level foreign key
/// (migration V2) still guards referential integrity.
///
/// Two time dimensions are kept apart: `transactionDate` is business time (when the
/// transaction happened in the world, supplied by the client), `createdAt` is system
/// time (when this system learned about it). AML rules operate on business time.
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false, length = 64)
    private String businessId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    private Instant transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
        // JPA only
    }

    private Transaction(UUID id, String businessId, UUID customerId, BigDecimal amount,
            String currency, Instant transactionDate, Instant createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
    }

    public static Transaction register(String businessId, UUID customerId, BigDecimal amount,
            String currency, Instant transactionDate, InstantSource clock) {
        // Scale is normalised once at the boundary; request validation already caps
        // fractions at 2 digits, so HALF_UP never rounds here - it documents policy.
        return new Transaction(Uuids.v7(), businessId, customerId,
                amount.setScale(2, RoundingMode.HALF_UP), currency, transactionDate, clock.instant());
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessId() {
        return businessId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
