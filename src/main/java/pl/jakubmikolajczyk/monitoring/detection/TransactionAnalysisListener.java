package pl.jakubmikolajczyk.monitoring.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final AmlRuleEngine engine;

    TransactionAnalysisListener(AmlRuleEngine engine) {
        this.engine = engine;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onTransactionRegistered(TransactionRegisteredEvent event) {
        log.info("AML analysis started eventId={} businessId={} transactionId={} on {}",
                event.eventId(), event.businessId(), event.transactionId(), Thread.currentThread());
        try {
            engine.analyse(event);
        } catch (DataIntegrityViolationException duplicateAlert) {
            // Two analyses raced for the same transaction; the database's unique
            // constraint is the source of truth and the first one won (ADR-0007).
            log.info("Alert for transaction {} already raised concurrently, skipping duplicate",
                    event.transactionId());
        } catch (RuntimeException analysisFailure) {
            // @Async failures never reach a caller; without this log they would
            // vanish into the default async exception handler.
            log.error("AML analysis failed eventId={} transactionId={}",
                    event.eventId(), event.transactionId(), analysisFailure);
        }
    }
}
