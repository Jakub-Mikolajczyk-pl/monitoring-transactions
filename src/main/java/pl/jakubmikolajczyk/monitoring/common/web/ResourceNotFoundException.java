package pl.jakubmikolajczyk.monitoring.common.web;

/// Thrown when a requested resource does not exist; mapped to HTTP 404 (ADR-0009).
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s %s not found".formatted(resource, id));
    }
}
