package pl.jakubmikolajczyk.monitoring.alert.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.alert.Alert;
import pl.jakubmikolajczyk.monitoring.alert.AlertStatus;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerResponse;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionResponse;

/// Everything the analyst's detail view needs in one response (REQ-04): the alert,
/// the transaction that triggered it, the customer behind it and the full decision
/// history, newest first.
@Schema(description = "Szczegóły alertu z transakcją, klientem i historią decyzji.")
public record AlertDetailsResponse(
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
        long version,
        @Schema(description = "Transakcja źródłowa alertu.")
        TransactionResponse transaction,
        @Schema(description = "Klient, którego dotyczy transakcja.")
        CustomerResponse customer,
        @Schema(description = "Historia decyzji analityka, od najnowszej do najstarszej.")
        List<DecisionResponse> decisions) {

    public static AlertDetailsResponse of(Alert alert, TransactionResponse transaction,
            CustomerResponse customer, List<DecisionResponse> decisions) {
        return new AlertDetailsResponse(
                alert.getId(),
                alert.getBusinessId(),
                alert.getTransactionId(),
                alert.getStatus(),
                alert.getReason(),
                alert.getCreatedAt(),
                alert.getVersion(),
                transaction,
                customer,
                decisions);
    }
}
