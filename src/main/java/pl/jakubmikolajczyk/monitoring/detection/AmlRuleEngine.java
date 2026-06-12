package pl.jakubmikolajczyk.monitoring.detection;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pl.jakubmikolajczyk.monitoring.alert.AlertService;
import pl.jakubmikolajczyk.monitoring.transaction.TransactionRegisteredEvent;
import pl.jakubmikolajczyk.monitoring.transaction.TransactionService;

/// Evaluates the whole rule catalogue against the (already committed) transaction
/// referenced by the event and raises a single merged alert when anything is
/// violated (ADR-0007). Spring injects every AmlRule bean - adding a rule means
/// adding a class plus one entry in the sealed interface's permits list.
@Component
public class AmlRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(AmlRuleEngine.class);

    private final List<AmlRule> rules;
    private final TransactionService transactions;
    private final AlertService alerts;

    AmlRuleEngine(List<AmlRule> rules, TransactionService transactions, AlertService alerts) {
        this.rules = rules;
        this.transactions = transactions;
        this.alerts = alerts;
    }

    public void analyse(TransactionRegisteredEvent event) {
        var transaction = transactions.findById(event.transactionId());

        // mapMulti + exhaustive switch over the sealed hierarchy: the compiler
        // guarantees every RuleResult variant is handled (Java 16 / Java 21).
        var violations = rules.stream()
                .map(rule -> rule.evaluate(transaction))
                .<RuleResult.Violation>mapMulti((result, downstream) -> {
                    switch (result) {
                        case RuleResult.Violation violation -> downstream.accept(violation);
                        case RuleResult.Clean ignored -> { /* nothing to report */ }
                    }
                })
                .toList();

        if (violations.isEmpty()) {
            log.info("AML analysis clean transactionId={}", event.transactionId());
            return;
        }
        violations.forEach(violation -> log.info("AML violation transactionId={} reason={} ({})",
                event.transactionId(), violation.reason(), violation.detail()));

        alerts.raise(transaction.getBusinessId(), transaction.getId(),
                violations.stream().map(RuleResult.Violation::reason).toList());
    }
}
