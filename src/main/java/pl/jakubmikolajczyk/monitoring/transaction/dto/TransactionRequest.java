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

import pl.jakubmikolajczyk.monitoring.common.validation.ValidCurrency;

public record TransactionRequest(
        @NotBlank @Size(max = 64) String businessId,
        @NotNull UUID customerId,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @ValidCurrency String currency,
        @NotNull @PastOrPresent Instant transactionDate) {
}
