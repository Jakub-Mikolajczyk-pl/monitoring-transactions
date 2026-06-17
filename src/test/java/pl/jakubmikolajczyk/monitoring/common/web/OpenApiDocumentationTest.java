package pl.jakubmikolajczyk.monitoring.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void exposesHumanReadableEndpointParameterAndSchemaDescriptions() throws Exception {
        var result = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        var openApiJson = result.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(openApiJson)
                .contains("\"summary\":\"Rejestruje klienta\"")
                .contains("\"summary\":\"Wyszukuje transakcje\"")
                .contains("\"description\":\"Identyfikator kontekstu biznesowego, np. BANK_A.\"")
                .contains("\"description\":\"Wersja alertu widziana przez analityka w momencie decyzji.\"")
                .contains("\"description\":\"Numer strony liczony od 0.\"");
    }
}
