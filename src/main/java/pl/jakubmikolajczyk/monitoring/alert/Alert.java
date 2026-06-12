package pl.jakubmikolajczyk.monitoring.alert;

import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import pl.jakubmikolajczyk.monitoring.common.id.Uuids;

/// Result of AML analysis for exactly one transaction (unique constraint in V3,
/// ADR-0007). `businessId` is inherited from the transaction so analysts can work
/// within their business context without joins. The `@Version` field backs
/// optimistic locking for concurrent analyst decisions (ADR-0008).
@Entity
@Table(name = "alerts")
public class Alert {

    private static final String REASON_SEPARATOR = ",";

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false, length = 64)
    private String businessId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Column(nullable = false, updatable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Alert() {
        // JPA only
    }

    private Alert(UUID id, String businessId, UUID transactionId, AlertStatus status,
            String reason, Instant createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.transactionId = transactionId;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static Alert raise(String businessId, UUID transactionId, List<AlertReason> reasons,
            InstantSource clock) {
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("An alert needs at least one reason");
        }
        var joinedReasons = reasons.stream()
                .map(Enum::name)
                .collect(Collectors.joining(REASON_SEPARATOR));
        return new Alert(Uuids.v7(), businessId, transactionId, AlertStatus.OPEN,
                joinedReasons, clock.instant());
    }

    /// Applies the analyst's verdict (ADR-0008). The status always mirrors the
    /// latest decision; the full history lives in alert_decisions. Hibernate's
    /// dirty checking turns this into an UPDATE that also bumps the version.
    public void applyDecision(Decision decision) {
        this.status = decision.resultingStatus();
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessId() {
        return businessId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
