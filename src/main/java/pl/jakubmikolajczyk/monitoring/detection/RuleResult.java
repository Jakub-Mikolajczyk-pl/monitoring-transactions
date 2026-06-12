package pl.jakubmikolajczyk.monitoring.detection;

import pl.jakubmikolajczyk.monitoring.alert.AlertReason;

/// Closed result hierarchy (ADR-0007): the engine consumes it with an exhaustive
/// `switch`, so forgetting to handle a variant is a compile-time error, not a
/// runtime surprise. Records keep each variant immutable and self-describing.
public sealed interface RuleResult {

    record Violation(AlertReason reason, String detail) implements RuleResult {
    }

    record Clean() implements RuleResult {
    }
}
