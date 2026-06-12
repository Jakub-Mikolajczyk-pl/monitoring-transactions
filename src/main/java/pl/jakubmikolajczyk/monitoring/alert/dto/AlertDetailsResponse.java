package pl.jakubmikolajczyk.monitoring.alert.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pl.jakubmikolajczyk.monitoring.alert.Alert;
import pl.jakubmikolajczyk.monitoring.alert.AlertStatus;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerResponse;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionResponse;

/// Everything the analyst's detail view needs in one response (REQ-04): the alert,
/// the transaction that triggered it, the customer behind it and the full decision
/// history, newest first.
public record AlertDetailsResponse(
        UUID id,
        String businessId,
        UUID transactionId,
        AlertStatus status,
        String reason,
        Instant createdAt,
        long version,
        TransactionResponse transaction,
        CustomerResponse customer,
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
