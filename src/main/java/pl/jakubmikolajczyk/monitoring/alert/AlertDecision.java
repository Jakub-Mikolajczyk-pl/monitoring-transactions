package pl.jakubmikolajczyk.monitoring.alert;

import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import pl.jakubmikolajczyk.monitoring.common.id.Uuids;

/// One analyst decision - append-only by design (REQ-11, ADR-0008): decisions are
/// never edited or deleted, the full history is the audit trail. The current alert
/// status is just a projection of the newest entry.
@Entity
@Table(name = "alert_decisions")
public class AlertDecision {

    @Id
    private UUID id;

    @Column(name = "alert_id", nullable = false, updatable = false)
    private UUID alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private Decision decision;

    @Column(nullable = false, updatable = false, length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AlertDecision() {
        // JPA only
    }

    private AlertDecision(UUID id, UUID alertId, Decision decision, String comment, Instant createdAt) {
        this.id = id;
        this.alertId = alertId;
        this.decision = decision;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public static AlertDecision of(UUID alertId, Decision decision, String comment, InstantSource clock) {
        return new AlertDecision(Uuids.v7(), alertId, decision, comment, clock.instant());
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
