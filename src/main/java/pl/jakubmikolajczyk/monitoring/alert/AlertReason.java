package pl.jakubmikolajczyk.monitoring.alert;

/// Catalogue of AML violation codes. Stored in the alert as a comma-joined string
/// to match the single `reason` field of the task's domain model (ADR-0007).
public enum AlertReason {
    SUSPICIOUS_AMOUNT,
    HIGH_FREQUENCY
}
