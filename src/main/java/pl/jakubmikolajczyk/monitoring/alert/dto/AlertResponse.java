package pl.jakubmikolajczyk.monitoring.alert.dto;

import java.time.Instant;
import java.util.UUID;

import pl.jakubmikolajczyk.monitoring.alert.Alert;
import pl.jakubmikolajczyk.monitoring.alert.AlertStatus;

/// `version` is part of the contract on purpose: clients echo it back when posting
/// a decision, which is how optimistic locking reaches the API level (ADR-0008).
public record AlertResponse(
        UUID id,
        String businessId,
        UUID transactionId,
        AlertStatus status,
        String reason,
        Instant createdAt,
        long version) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getBusinessId(),
                alert.getTransactionId(),
                alert.getStatus(),
                alert.getReason(),
                alert.getCreatedAt(),
                alert.getVersion());
    }
}
