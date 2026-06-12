package pl.jakubmikolajczyk.monitoring.common.web;

import java.util.Map;
import java.util.Objects;

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

    /// Enriches the standard 400 response with per-field validation errors so API
    /// clients (including our own UI) can point at the offending fields.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
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
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
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
}
