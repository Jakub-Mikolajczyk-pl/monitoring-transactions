package pl.jakubmikolajczyk.monitoring.detection;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import pl.jakubmikolajczyk.monitoring.alert.AlertReason;
import pl.jakubmikolajczyk.monitoring.transaction.Transaction;

class SuspiciousAmountRuleTest {

    private final SuspiciousAmountRule rule = new SuspiciousAmountRule(
            new AmlProperties(
                    new AmlProperties.SuspiciousAmount(new BigDecimal("2000.00")),
                    new AmlProperties.HighFrequency(5, Duration.ofHours(1))));

    private Transaction transactionOf(String amount) {
        return Transaction.register("BANK_A", UUID.randomUUID(), new BigDecimal(amount), "PLN",
                Instant.parse("2026-06-01T10:00:00Z"),
                Clock.fixed(Instant.parse("2026-06-01T10:05:00Z"), ZoneOffset.UTC));
    }

    @Test
    void staysCleanExactlyAtTheThreshold() {
        // "exceeds" is a strict inequality (REQ-09)
        assertThat(rule.evaluate(transactionOf("2000.00"))).isInstanceOf(RuleResult.Clean.class);
    }

    @Test
    void flagsAmountStrictlyAboveTheThreshold() {
        var result = rule.evaluate(transactionOf("2000.01"));

        // record pattern (Java 21): deconstruct and assert in one move
        if (result instanceof RuleResult.Violation(AlertReason reason, String detail)) {
            assertThat(reason).isEqualTo(AlertReason.SUSPICIOUS_AMOUNT);
            assertThat(detail).contains("2000.01");
        } else {
            org.assertj.core.api.Assertions.fail("Expected a violation, got: " + result);
        }
    }

    @Test
    void comparesByValueNotByScale() {
        // 2000.0 == 2000.00 for compareTo; BigDecimal.equals would disagree
        assertThat(rule.evaluate(transactionOf("2000.0"))).isInstanceOf(RuleResult.Clean.class);
    }
}
