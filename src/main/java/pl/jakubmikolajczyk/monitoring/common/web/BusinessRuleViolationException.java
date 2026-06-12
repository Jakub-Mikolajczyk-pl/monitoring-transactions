package pl.jakubmikolajczyk.monitoring.common.web;

/// Request is syntactically fine but breaks a domain consistency rule; mapped to
/// HTTP 422 (ADR-0009).
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
