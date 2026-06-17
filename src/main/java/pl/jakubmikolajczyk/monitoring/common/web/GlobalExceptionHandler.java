package pl.jakubmikolajczyk.monitoring.common.web;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/// Single place where errors become RFC 9457 `application/problem+json` responses
/// (ADR-0009). Extending [ResponseEntityExceptionHandler] gives every framework-level
/// exception (malformed JSON, missing parameters, unsupported media type...) a
/// `ProblemDetail` body for free; domain exceptions are mapped explicitly below.
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /// Enriches the standard 400 response with per-field validation errors so API
    /// clients (including our own UI) can point at the offending fields.
    @Override
    @SuppressWarnings("java:S2638")
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value")))
                .toList());
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    /// Syntactically valid request breaking a domain consistency rule -> 422.
    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Business rule violated");
        return problem;
    }

    /// Defensive-programming failures while handling a request (e.g. an inverted
    /// date range in search criteria) are the caller's fault -> 400.
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /// Lost-update protection (ADR-0008): either the client sent a stale version
    /// (ConflictException) or two writes raced at flush time (JPA optimistic lock).
    /// Both mean the same thing to the caller: reload and try again -> 409.
    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
    ProblemDetail handleConflict(RuntimeException ex) {
        var detail = ex instanceof ConflictException
                ? ex.getMessage()
                : "The resource was modified concurrently; reload and retry";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setTitle("Concurrent modification");
        return problem;
    }

    /// Last-resort safety net for anything not mapped above. The full stack trace is
    /// logged server-side under a correlation id; the client only gets that id and a
    /// generic message, never internal details (no stack traces or messages that could
    /// leak implementation or data). This keeps the error contract RFC 9457 end to end
    /// instead of falling back to the servlet container's default error page.
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled exception [correlationId={}]", correlationId, ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Quote this id when reporting it: " + correlationId);
        problem.setTitle("Internal error");
        problem.setProperty("correlationId", correlationId);
        return problem;
    }
}
