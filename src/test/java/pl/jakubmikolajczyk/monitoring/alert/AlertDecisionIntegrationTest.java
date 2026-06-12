package pl.jakubmikolajczyk.monitoring.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

/// REQ-04, REQ-05, REQ-11 and the optimistic-locking contract of ADR-0008.
/// The stale-version scenario is deliberately deterministic: instead of racing two
/// threads we replay a version the server has already moved past.
@SpringBootTest
@AutoConfigureMockMvc
class AlertDecisionIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    private String registerCustomer(String businessId) throws Exception {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "%s", "firstName": "Piotr", "lastName": "Decydent"}
                        """.formatted(businessId))
                .exchange();
        return JsonPath.read(result.getMvcResult().getResponse().getContentAsString(), "$.id");
    }

    private String registerSuspiciousTransaction(String businessId, String customerId) throws Exception {
        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "%s",
                          "customerId": "%s",
                          "amount": 2500.00,
                          "currency": "PLN",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """.formatted(businessId, customerId))
                .exchange();
        return JsonPath.read(result.getMvcResult().getResponse().getContentAsString(), "$.id");
    }

    /// Seeds a full context and waits for the asynchronous alert to materialise.
    private Map<String, Object> raiseAlert(String businessId) throws Exception {
        String customerId = registerCustomer(businessId);
        String transactionId = registerSuspiciousTransaction(businessId, customerId);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(alertsFor(transactionId)).hasSize(1));
        return alertsFor(transactionId).getFirst();
    }

    private List<Map<String, Object>> alertsFor(String transactionId) throws Exception {
        var result = mvc.get().uri("/api/alerts").exchange();
        String body = result.getMvcResult().getResponse().getContentAsString();
        return JsonPath.read(body, "$[?(@.transactionId == '%s')]".formatted(transactionId));
    }

    private MvcTestResult postDecision(Object alertId, String decision, String comment, long version) {
        return mvc.post().uri("/api/alerts/%s/decisions".formatted(alertId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"decision": "%s", "comment": "%s", "alertVersion": %d}
                        """.formatted(decision, comment, version))
                .exchange();
    }

    @Test
    void recordsApprovalMovesStatusAndExposesFullDetails() throws Exception {
        var alert = raiseAlert("DEC_A");

        var decisionResult = postDecision(alert.get("id"), "APPROVE", "Verified with the customer", 0);
        assertThat(decisionResult).hasStatus(HttpStatus.CREATED);
        assertThat(decisionResult).bodyJson().extractingPath("$.decision").isEqualTo("APPROVE");

        var details = mvc.get().uri("/api/alerts/" + alert.get("id")).exchange();
        assertThat(details).hasStatus(HttpStatus.OK);
        assertThat(details).bodyJson().extractingPath("$.status").isEqualTo("APPROVED");
        assertThat(details).bodyJson().extractingPath("$.version").isEqualTo(1);
        assertThat(details).bodyJson().extractingPath("$.decisions.length()").isEqualTo(1);
        assertThat(details).bodyJson().extractingPath("$.transaction.amount").isEqualTo(2500.00);
        assertThat(details).bodyJson().extractingPath("$.customer.firstName").isEqualTo("Piotr");
    }

    @Test
    void rejectsDecisionBasedOnStaleVersionWith409() throws Exception {
        var alert = raiseAlert("DEC_B");
        postDecision(alert.get("id"), "APPROVE", "First analyst was here", 0);

        // Second analyst still looks at version 0 - the server has moved to 1.
        var stale = postDecision(alert.get("id"), "REJECT", "Working off a stale view", 0);

        assertThat(stale).hasStatus(HttpStatus.CONFLICT);
        assertThat(stale).bodyJson().extractingPath("$.title").isEqualTo("Concurrent modification");

        var details = mvc.get().uri("/api/alerts/" + alert.get("id")).exchange();
        assertThat(details).bodyJson().extractingPath("$.status").isEqualTo("APPROVED");
        assertThat(details).bodyJson().extractingPath("$.decisions.length()").isEqualTo(1);
    }

    @Test
    void allowsReReviewWithFreshVersionAndKeepsFullHistory() throws Exception {
        var alert = raiseAlert("DEC_C");
        postDecision(alert.get("id"), "APPROVE", "Initial approval", 0);

        var reReview = postDecision(alert.get("id"), "REJECT", "New evidence arrived", 1);
        assertThat(reReview).hasStatus(HttpStatus.CREATED);

        var details = mvc.get().uri("/api/alerts/" + alert.get("id")).exchange();
        assertThat(details).bodyJson().extractingPath("$.status").isEqualTo("REJECTED");
        assertThat(details).bodyJson().extractingPath("$.decisions.length()").isEqualTo(2);
        // newest first
        assertThat(details).bodyJson().extractingPath("$.decisions[0].decision").isEqualTo("REJECT");
        assertThat(details).bodyJson().extractingPath("$.decisions[1].decision").isEqualTo("APPROVE");
    }

    @Test
    void requiresJustifyingComment() throws Exception {
        var alert = raiseAlert("DEC_D");

        var result = postDecision(alert.get("id"), "APPROVE", "   ", 0);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("comment");
    }

    @Test
    void returns404ForDecisionOnUnknownAlert() {
        var result = postDecision("00000000-0000-7000-8000-000000000000", "APPROVE", "ghost", 0);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
