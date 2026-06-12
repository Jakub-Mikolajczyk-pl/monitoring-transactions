package pl.jakubmikolajczyk.monitoring.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionApiIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    private String registerCustomer(String businessId) throws Exception {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "%s", "firstName": "Jan", "lastName": "Testowy"}
                        """.formatted(businessId))
                .exchange();
        return JsonPath.read(result.getMvcResult().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void registersTransactionForExistingCustomer() throws Exception {
        String customerId = registerCustomer("TXN_A");

        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "TXN_A",
                          "customerId": "%s",
                          "amount": 1500.75,
                          "currency": "PLN",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """.formatted(customerId))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.amount").isEqualTo(1500.75);
        assertThat(result).bodyJson().extractingPath("$.currency").isEqualTo("PLN");
        assertThat(result.getMvcResult().getResponse().getHeader(HttpHeaders.LOCATION))
                .contains("/api/transactions/");
    }

    @Test
    void rejectsTransactionForUnknownCustomerAsUnprocessable() {
        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "TXN_B",
                          "customerId": "00000000-0000-7000-8000-000000000000",
                          "amount": 10.00,
                          "currency": "PLN",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Business rule violated");
    }

    @Test
    void rejectsTransactionWhoseBusinessContextDiffersFromCustomers() throws Exception {
        String customerId = registerCustomer("TXN_C");

        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "TXN_OTHER",
                          "customerId": "%s",
                          "amount": 10.00,
                          "currency": "PLN",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """.formatted(customerId))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .asString().contains("does not match customer businessId");
    }

    @Test
    void rejectsUnknownCurrencyWithFieldLevelError() throws Exception {
        String customerId = registerCustomer("TXN_D");

        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "TXN_D",
                          "customerId": "%s",
                          "amount": 10.00,
                          "currency": "ZZZ",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """.formatted(customerId))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("currency");
    }

    @Test
    void rejectsNonPositiveAmount() throws Exception {
        String customerId = registerCustomer("TXN_E");

        var result = mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessId": "TXN_E",
                          "customerId": "%s",
                          "amount": -5.00,
                          "currency": "PLN",
                          "transactionDate": "2026-06-01T14:30:00Z"
                        }
                        """.formatted(customerId))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("amount");
    }
}
