package pl.jakubmikolajczyk.monitoring.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pl.jakubmikolajczyk.monitoring.transaction.TransactionRegisteredEvent;

/// The "separate component" demanded by §4.3: it reacts to committed transactions
/// and runs AML analysis off the caller's thread. AFTER_COMMIT guarantees the
/// transaction is durable before any rule reads it; @Async moves the work onto a
/// virtual thread, so registration latency never includes analysis.
@Component
class TransactionAnalysisListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionAnalysisListener.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onTransactionRegistered(TransactionRegisteredEvent event) {
        log.info("AML analysis requested eventId={} businessId={} transactionId={} on {}",
                event.eventId(), event.businessId(), event.transactionId(), Thread.currentThread());
        // Rule evaluation plugs in here in the next increment.
    }
}
