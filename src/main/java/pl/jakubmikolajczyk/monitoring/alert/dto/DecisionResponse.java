package pl.jakubmikolajczyk.monitoring.alert.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.alert.AlertDecision;
import pl.jakubmikolajczyk.monitoring.alert.Decision;

@Schema(description = "Pojedynczy wpis w historii decyzji alertu.")
public record DecisionResponse(
        @Schema(description = "Techniczny identyfikator decyzji (UUIDv7).", example = "0190abcd-1234-7000-8000-000000000030")
        UUID id,
        @Schema(description = "Alert, którego dotyczy decyzja.", example = "0190abcd-1234-7000-8000-000000000020")
        UUID alertId,
        @Schema(description = "Werdykt analityka.")
        Decision decision,
        @Schema(description = "Uzasadnienie decyzji.", example = "Zweryfikowano z klientem.")
        String comment,
        @Schema(description = "Czas zapisania decyzji.", example = "2026-06-11T10:35:00Z")
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
