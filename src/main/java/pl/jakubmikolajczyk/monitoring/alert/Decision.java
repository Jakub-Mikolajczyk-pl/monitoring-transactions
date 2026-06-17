package pl.jakubmikolajczyk.monitoring.alert;

import io.swagger.v3.oas.annotations.media.Schema;

/// Analyst's verdict on an alert (REQ-11). Each verdict knows which alert status
/// it produces - an exhaustive switch expression keeps the mapping total.
@Schema(description = "Werdykt analityka: APPROVE zatwierdza alert, REJECT go odrzuca.")
public enum Decision {
    APPROVE,
    REJECT;

    public AlertStatus resultingStatus() {
        return switch (this) {
            case APPROVE -> AlertStatus.APPROVED;
            case REJECT -> AlertStatus.REJECTED;
        };
    }
}
