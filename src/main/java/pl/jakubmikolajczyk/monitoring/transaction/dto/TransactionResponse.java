package pl.jakubmikolajczyk.monitoring.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import pl.jakubmikolajczyk.monitoring.transaction.Transaction;

public record TransactionResponse(
        UUID id,
        String businessId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        Instant transactionDate,
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
