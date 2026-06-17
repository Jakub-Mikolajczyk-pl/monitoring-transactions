package pl.jakubmikolajczyk.monitoring.alert.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.alert.Alert;
import pl.jakubmikolajczyk.monitoring.alert.AlertStatus;

/// `version` is part of the contract on purpose: clients echo it back when posting
/// a decision, which is how optimistic locking reaches the API level (ADR-0008).
@Schema(description = "Alert AML widoczny w kolejce analityka.")
public record AlertResponse(
        @Schema(description = "Techniczny identyfikator alertu (UUIDv7).", example = "0190abcd-1234-7000-8000-000000000020")
        UUID id,
        @Schema(description = "Identyfikator kontekstu biznesowego, np. BANK_A.", example = "BANK_A")
        String businessId,
        @Schema(description = "Transakcja, która wywołała alert.", example = "0190abcd-1234-7000-8000-000000000010")
        UUID transactionId,
        @Schema(description = "Aktualny status alertu.")
        AlertStatus status,
        @Schema(description = "Powód lub powody alertu rozdzielone przecinkiem.", example = "SUSPICIOUS_AMOUNT,HIGH_FREQUENCY")
        String reason,
        @Schema(description = "Czas utworzenia alertu.", example = "2026-06-11T10:30:03Z")
        Instant createdAt,
        @Schema(description = "Wersja alertu używana do wykrywania utraconych aktualizacji.", example = "0")
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
