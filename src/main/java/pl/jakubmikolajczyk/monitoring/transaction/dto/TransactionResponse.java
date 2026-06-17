package pl.jakubmikolajczyk.monitoring.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.transaction.Transaction;

@Schema(description = "Niemutowalna transakcja zapisana w systemie.")
public record TransactionResponse(
        @Schema(description = "Techniczny identyfikator transakcji (UUIDv7).", example = "0190abcd-1234-7000-8000-000000000010")
        UUID id,
        @Schema(description = "Identyfikator kontekstu biznesowego, np. BANK_A.", example = "BANK_A")
        String businessId,
        @Schema(description = "Techniczny identyfikator klienta.", example = "0190abcd-1234-7000-8000-000000000001")
        UUID customerId,
        @Schema(description = "Kwota transakcji.", example = "2500.50")
        BigDecimal amount,
        @Schema(description = "Kod waluty ISO 4217.", example = "PLN")
        String currency,
        @Schema(description = "Czas biznesowy transakcji.", example = "2026-06-11T10:30:00Z")
        Instant transactionDate,
        @Schema(description = "Czas technicznego zapisu transakcji w systemie.", example = "2026-06-11T10:30:02Z")
        Instant createdAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getBusinessId(),
                transaction.getCustomerId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt());
    }
}
