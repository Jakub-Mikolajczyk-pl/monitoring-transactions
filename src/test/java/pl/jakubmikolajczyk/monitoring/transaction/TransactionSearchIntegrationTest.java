package pl.jakubmikolajczyk.monitoring.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;

/// Each test works inside its own business context (unique businessId), so tests
/// stay independent even though the Spring context - and the H2 instance - is shared.
@SpringBootTest
@AutoConfigureMockMvc
class TransactionSearchIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    private String registerCustomer(String businessId) throws Exception {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "%s", "firstName": "Anna", "lastName": "Testowa"}
                        """.formatted(businessId))
                .exchange();
        return JsonPath.read(result.getMvcResult().getResponse().getContentAsString(), "$.id");
    }

    private void registerTransaction(String businessId, String customerId, String amount, String dateIso) {
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
    }

    @Test
    void requiresBusinessIdFilter() {
        var result = mvc.get().uri("/api/transactions").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(400);
    }

    @Test
    void filtersByBusinessContextAndSortsNewestFirst() throws Exception {
        String inContext = registerCustomer("SRCH_A");
        String outsideContext = registerCustomer("SRCH_B");
        registerTransaction("SRCH_A", inContext, "10.00", "2026-06-01T10:00:00Z");
        registerTransaction("SRCH_A", inContext, "20.00", "2026-06-05T10:00:00Z");
        registerTransaction("SRCH_B", outsideContext, "99.00", "2026-06-03T10:00:00Z");

        var result = mvc.get().uri("/api/transactions?businessId=SRCH_A").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$[0].amount").isEqualTo(20.00);
        assertThat(result).bodyJson().extractingPath("$[1].amount").isEqualTo(10.00);
    }

    @Test
    void narrowsDownByCustomerWithinContext() throws Exception {
        String first = registerCustomer("SRCH_C");
        String second = registerCustomer("SRCH_C");
        registerTransaction("SRCH_C", first, "10.00", "2026-06-01T10:00:00Z");
        registerTransaction("SRCH_C", second, "20.00", "2026-06-02T10:00:00Z");

        var result = mvc.get()
                .uri("/api/transactions?businessId=SRCH_C&customerId=" + first)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$[0].customerId").isEqualTo(first);
    }

    @Test
    void narrowsDownByDateRange() throws Exception {
        String customerId = registerCustomer("SRCH_D");
        registerTransaction("SRCH_D", customerId, "10.00", "2026-06-01T10:00:00Z");
        registerTransaction("SRCH_D", customerId, "20.00", "2026-06-05T10:00:00Z");
        registerTransaction("SRCH_D", customerId, "30.00", "2026-06-09T10:00:00Z");

        var result = mvc.get()
                .uri("/api/transactions?businessId=SRCH_D"
                        + "&dateFrom=2026-06-03T00:00:00Z&dateTo=2026-06-07T00:00:00Z")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$[0].amount").isEqualTo(20.00);
    }

    @Test
    void rejectsInvertedDateRange() {
        var result = mvc.get()
                .uri("/api/transactions?businessId=SRCH_E"
                        + "&dateFrom=2026-06-07T00:00:00Z&dateTo=2026-06-03T00:00:00Z")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Invalid request");
    }
}
