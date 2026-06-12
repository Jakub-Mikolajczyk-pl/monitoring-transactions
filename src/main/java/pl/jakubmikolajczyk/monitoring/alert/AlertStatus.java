package pl.jakubmikolajczyk.monitoring.alert;

/// Alert lifecycle: born OPEN, then reflects the latest analyst decision
/// (ADR-0008). The full decision history lives in alert_decisions.
public enum AlertStatus {
    OPEN,
    APPROVED,
    REJECTED
}
