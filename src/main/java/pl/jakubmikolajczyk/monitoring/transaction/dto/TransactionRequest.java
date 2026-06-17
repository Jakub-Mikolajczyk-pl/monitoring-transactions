package pl.jakubmikolajczyk.monitoring.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.common.validation.ValidCurrency;

@Schema(description = "Dane wymagane do rejestracji niemutowalnej transakcji.")
public record TransactionRequest(
        @Schema(description = "Identyfikator kontekstu biznesowego, np. BANK_A.", example = "BANK_A", maxLength = 64)
        @NotBlank @Size(max = 64) String businessId,
        @Schema(description = "Techniczny identyfikator klienta, którego dotyczy transakcja.", example = "0190abcd-1234-7000-8000-000000000001")
        @NotNull UUID customerId,
        @Schema(description = "Kwota transakcji z dokładnością do dwóch miejsc po przecinku.", example = "2500.50")
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Schema(description = "Kod waluty ISO 4217.", example = "PLN", pattern = "[A-Z]{3}")
        @NotNull @ValidCurrency String currency,
        @Schema(description = "Czas biznesowy transakcji; reguły AML liczą okna względem tej daty.", example = "2026-06-11T10:30:00Z")
        @NotNull @PastOrPresent Instant transactionDate) {
}
