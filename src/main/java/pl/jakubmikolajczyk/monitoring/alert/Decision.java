package pl.jakubmikolajczyk.monitoring.alert;

/// Analyst's verdict on an alert (REQ-11). Each verdict knows which alert status
/// it produces - an exhaustive switch expression keeps the mapping total.
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
