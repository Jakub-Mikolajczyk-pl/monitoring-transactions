package pl.jakubmikolajczyk.monitoring.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/// API contract documentation served at `/swagger-ui.html` (springdoc). The spec is
/// generated from code, so it cannot drift from the actual controllers.
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI apiDescription() {
        return new OpenAPI().info(new Info()
                .title("Transaction Monitoring (AML) API")
                .description("Customers, immutable transactions, asynchronous AML analysis, "
                        + "alerts and append-only analyst decisions.")
                .version("v1"));
    }
}
