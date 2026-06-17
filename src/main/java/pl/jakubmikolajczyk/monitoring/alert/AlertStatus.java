package pl.jakubmikolajczyk.monitoring.alert;

import io.swagger.v3.oas.annotations.media.Schema;

/// Alert lifecycle: born OPEN, then reflects the latest analyst decision
/// (ADR-0008). The full decision history lives in alert_decisions.
@Schema(description = "Status alertu: otwarty albo wynik ostatniej decyzji analityka.")
public enum AlertStatus {
    OPEN,
    APPROVED,
    REJECTED
}
