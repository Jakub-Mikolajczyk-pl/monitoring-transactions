package pl.jakubmikolajczyk.monitoring.alert.dto;

import java.time.Instant;
import java.util.UUID;

import pl.jakubmikolajczyk.monitoring.alert.AlertDecision;
import pl.jakubmikolajczyk.monitoring.alert.Decision;

public record DecisionResponse(
        UUID id,
        UUID alertId,
        Decision decision,
        String comment,
        Instant createdAt) {

    public static DecisionResponse from(AlertDecision decision) {
        return new DecisionResponse(
                decision.getId(),
                decision.getAlertId(),
                decision.getDecision(),
                decision.getComment(),
                decision.getCreatedAt());
    }
}
