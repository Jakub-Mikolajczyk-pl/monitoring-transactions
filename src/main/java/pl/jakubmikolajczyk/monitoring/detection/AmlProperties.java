package pl.jakubmikolajczyk.monitoring.detection;

import java.math.BigDecimal;
import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/// Rule parameters bound from application.properties into an immutable record
/// (constructor binding) and validated at startup - a configuration typo stops the
/// application from booting instead of silently disabling a rule (REQ-09, REQ-10).
@ConfigurationProperties(prefix = "aml.rules")
@Validated
public record AmlProperties(
        @NotNull @Valid SuspiciousAmount suspiciousAmount,
        @NotNull @Valid HighFrequency highFrequency) {

    public record SuspiciousAmount(@NotNull @Positive BigDecimal threshold) {
    }

    public record HighFrequency(@Positive int maxTransactions, @NotNull Duration window) {
    }
}
