package pl.jakubmikolajczyk.monitoring.transaction;

import java.util.UUID;

import pl.jakubmikolajczyk.monitoring.common.id.Uuids;

/// Integration event with exactly the payload required by the task (§4.3):
/// `{eventId, businessId, transactionId}`. It deliberately carries identifiers
/// only - consumers load the state they need at their own consistency point
/// (after commit), so the event can never smuggle uncommitted data (ADR-0006).
public record TransactionRegisteredEvent(UUID eventId, String businessId, UUID transactionId) {

    public static TransactionRegisteredEvent of(Transaction transaction) {
        return new TransactionRegisteredEvent(Uuids.v7(), transaction.getBusinessId(), transaction.getId());
    }
}
