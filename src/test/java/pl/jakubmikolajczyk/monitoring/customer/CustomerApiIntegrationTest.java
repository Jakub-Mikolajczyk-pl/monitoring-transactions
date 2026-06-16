package pl.jakubmikolajczyk.monitoring.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerApiIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void registersCustomerAndPointsAtItWithLocationHeader() {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "BANK_A", "firstName": "Jan", "lastName": "Kowalski"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.businessId").isEqualTo("BANK_A");
        assertThat(result).bodyJson().extractingPath("$.firstName").isEqualTo("Jan");

        String location = result.getMvcResult().getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).contains("/api/customers/");
    }

    @Test
    void exposesRegisteredCustomerOnTheList() throws Exception {
        mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "LIST_CHECK", "firstName": "Anna", "lastName": "Nowak"}
                        """)
                .exchange();

        var result = mvc.get().uri("/api/customers").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getMvcResult().getResponse().getContentAsString()).contains("LIST_CHECK");
    }

    @Test
    void rejectsBlankFirstNameWithFieldLevelProblemDetail() {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "BANK_A", "firstName": "   ", "lastName": "Kowalski"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("firstName");
    }

    @Test
    void returnsProblemDetailForUnknownCustomer() {
        var result = mvc.get()
                .uri("/api/customers/00000000-0000-7000-8000-000000000000")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void storesValidEmailWhenProvided() {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "BANK_A", "firstName": "Ewa", "lastName": "Mejlowa",
                         "email": "ewa.mejlowa@example.com"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("ewa.mejlowa@example.com");
    }

    @Test
    void acceptsCustomerWithoutEmail() {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "BANK_A", "firstName": "Bez", "lastName": "Maila"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.email").isNull();
    }

    @Test
    void rejectsMalformedEmailWithFieldLevelProblemDetail() {
        var result = mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessId": "BANK_A", "firstName": "Zła", "lastName": "Składnia",
                         "email": "not-an-email"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("email");
    }
}
