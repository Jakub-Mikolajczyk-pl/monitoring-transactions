package pl.jakubmikolajczyk.monitoring.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class TransactionEventTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ApplicationEvents events;

    @Test
    void publishesEventWithTaskMandatedPayloadAfterRegistration() throws Exception {
        var customerResult = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "EVT_A", "firstName": "Ewa", "lastName": "Eventowa"}
                        """)
                .exchange();
        String customerId = JsonPath.read(
                customerResult.getMvcResult().getResponse().getContentAsString(), "$.id");

        var transactionResult = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "EVT_A",
                          "customerId": "%s",
                          "amount": 42.00,
                          "currency": "PLN",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """.formatted(customerId))
                .exchange();
        String transactionId = JsonPath.read(
                transactionResult.getMvcResult().getResponse().getContentAsString(), "$.id");

        var published = events.stream(TransactionRegisteredEvent.class).toList();

        assertThat(published).hasSize(1);
        var event = published.getFirst(); // SequencedCollection, Java 21
        assertThat(event.eventId()).isNotNull();
        assertThat(event.businessId()).isEqualTo("EVT_A");
        assertThat(event.transactionId()).hasToString(transactionId);
    }
}
