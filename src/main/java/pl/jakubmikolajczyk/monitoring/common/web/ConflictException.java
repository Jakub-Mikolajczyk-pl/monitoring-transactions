package pl.jakubmikolajczyk.monitoring.common.web;

/// The request was based on a state of the resource that has changed in the
/// meantime; mapped to HTTP 409 (ADR-0008). The client should reload and retry.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
