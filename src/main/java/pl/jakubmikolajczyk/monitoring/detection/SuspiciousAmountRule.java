package pl.jakubmikolajczyk.monitoring.detection;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import pl.jakubmikolajczyk.monitoring.alert.AlertReason;
import pl.jakubmikolajczyk.monitoring.transaction.Transaction;

/// REQ-09: flags transactions whose amount strictly exceeds the configured
/// threshold. Comparison uses compareTo, never equals - BigDecimal equals treats
/// 2000.0 and 2000.00 as different values (ADR-0004).
@Component
final class SuspiciousAmountRule implements AmlRule {

    private final BigDecimal threshold;

    SuspiciousAmountRule(AmlProperties properties) {
        this.threshold = properties.suspiciousAmount().threshold();
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        if (transaction.getAmount().compareTo(threshold) > 0) {
            return new RuleResult.Violation(AlertReason.SUSPICIOUS_AMOUNT,
                    "amount %s exceeds threshold %s".formatted(transaction.getAmount(), threshold));
        }
        return new RuleResult.Clean();
    }
}
