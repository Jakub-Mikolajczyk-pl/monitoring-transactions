package pl.jakubmikolajczyk.monitoring.detection;

import pl.jakubmikolajczyk.monitoring.transaction.Transaction;

/// A single AML rule evaluated against a committed transaction.
///
/// The interface is sealed on purpose: in a bank the rule catalogue is a governed,
/// auditable artefact - adding a rule should be an explicit, reviewable change to
/// this very file, not a class appearing somewhere on the classpath (ADR-0007).
/// Spring still discovers the implementations and injects them as a `List<AmlRule>`.
public sealed interface AmlRule permits SuspiciousAmountRule {

    RuleResult evaluate(Transaction transaction);
}
