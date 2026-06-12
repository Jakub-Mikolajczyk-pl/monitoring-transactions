package pl.jakubmikolajczyk.monitoring.detection;

import java.time.Duration;

import org.springframework.stereotype.Component;

import pl.jakubmikolajczyk.monitoring.alert.AlertReason;
import pl.jakubmikolajczyk.monitoring.transaction.Transaction;
import pl.jakubmikolajczyk.monitoring.transaction.TransactionRepository;

/// REQ-10: flags a customer who made more than `maxTransactions` transactions
/// within `window`. The window is anchored to the analysed transaction's business
/// time (ADR-0005), so a delayed analysis can never change the verdict. The count
/// includes the analysed transaction itself - it is committed before AFTER_COMMIT
/// listeners run, so the sixth transaction in an hour reliably counts as 6.
@Component
final class HighFrequencyRule implements AmlRule {

    private final int maxTransactions;
    private final Duration window;
    private final TransactionRepository transactions;

    HighFrequencyRule(AmlProperties properties, TransactionRepository transactions) {
        this.maxTransactions = properties.highFrequency().maxTransactions();
        this.window = properties.highFrequency().window();
        this.transactions = transactions;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        var windowEnd = transaction.getTransactionDate();
        var windowStart = windowEnd.minus(window);
        long count = transactions.countInWindow(transaction.getCustomerId(), windowStart, windowEnd);
        if (count > maxTransactions) {
            return new RuleResult.Violation(AlertReason.HIGH_FREQUENCY,
                    "%d transactions within %s (limit: %d)".formatted(count, window, maxTransactions));
        }
        return new RuleResult.Clean();
    }
}
