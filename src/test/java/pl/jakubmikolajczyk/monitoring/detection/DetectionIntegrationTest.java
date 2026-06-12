package pl.jakubmikolajczyk.monitoring.detection;

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

import com.jayway.jsonpath.JsonPath;

/// End-to-end proof of REQ-08/REQ-09: registering a transaction returns
/// immediately, and the alert materialises asynchronously (Awaitility, no sleeps).
@SpringBootTest
@AutoConfigureMockMvc
class DetectionIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    private String registerCustomer(String businessId) throws Exception {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "%s", "firstName": "Olga", "lastName": "Analityczna"}
                        """.formatted(businessId))
                .exchange();
        return JsonPath.read(result.getMvcResult().getResponse().getContentAsString(), "$.id");
    }

    private String registerTransaction(String businessId, String customerId, String amount,
            String dateIso) throws Exception {
        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "%s",
                          "customerId": "%s",
                          "amount": %s,
                          "currency": "PLN",
                          "transactionDate": "%s"
                        }
                        """.formatted(businessId, customerId, amount, dateIso))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(result.getMvcResult().getResponse().getContentAsString(), "$.id");
    }

    private List<Map<String, Object>> alertsFor(String transactionId) throws Exception {
        var result = mvc.get().uri("/api/alerts").exchange();
        String body = result.getMvcResult().getResponse().getContentAsString();
        return JsonPath.read(body, "$[?(@.transactionId == '%s')]".formatted(transactionId));
    }

    @Test
    void raisesOpenAlertForSuspiciousAmountAsynchronously() throws Exception {
        String customerId = registerCustomer("DET_A");
        String transactionId = registerTransaction("DET_A", customerId, "2500.50",
                "2026-06-01T14:30:00Z");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(alertsFor(transactionId)).hasSize(1));

        var alert = alertsFor(transactionId).getFirst();
        assertThat(alert.get("status")).isEqualTo("OPEN");
        assertThat(alert.get("reason")).isEqualTo("SUSPICIOUS_AMOUNT");
        assertThat(alert.get("businessId")).isEqualTo("DET_A");
    }

    @Test
    void leavesUnsuspiciousTransactionWithoutAlert() throws Exception {
        String customerId = registerCustomer("DET_B");
        String cleanId = registerTransaction("DET_B", customerId, "100.00",
                "2026-06-01T14:00:00Z");
        String suspiciousId = registerTransaction("DET_B", customerId, "3000.00",
                "2026-06-01T14:31:00Z");

        // The suspicious alert is the synchronisation point: once it exists, we can
        // meaningfully assert the clean transaction produced nothing.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(alertsFor(suspiciousId)).hasSize(1));

        assertThat(alertsFor(cleanId)).isEmpty();
    }
}
