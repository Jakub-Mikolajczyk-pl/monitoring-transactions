package pl.jakubmikolajczyk.monitoring.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedExceptionBecomesSanitizedProblemDetail() {
        var leakyCause = new IllegalStateException("jdbc url=secret password=hunter2");

        ProblemDetail problem = handler.handleUnexpected(leakyCause);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal error");
        // The internal message must never reach the client.
        assertThat(problem.getDetail()).doesNotContain("hunter2", "jdbc");
        // A correlation id ties the opaque client response to the full server-side log.
        assertThat(problem.getProperties()).containsKey("correlationId");
        assertThat(problem.getDetail()).contains((String) problem.getProperties().get("correlationId"));
    }
}
