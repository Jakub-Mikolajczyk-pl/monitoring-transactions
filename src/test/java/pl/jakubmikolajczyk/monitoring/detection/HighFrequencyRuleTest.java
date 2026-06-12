package pl.jakubmikolajczyk.monitoring.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import pl.jakubmikolajczyk.monitoring.alert.AlertReason;
import pl.jakubmikolajczyk.monitoring.transaction.Transaction;
import pl.jakubmikolajczyk.monitoring.transaction.TransactionRepository;

class HighFrequencyRuleTest {

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-01T14:30:00Z");

    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final HighFrequencyRule rule = new HighFrequencyRule(
            new AmlProperties(
                    new AmlProperties.SuspiciousAmount(new BigDecimal("2000.00")),
                    new AmlProperties.HighFrequency(5, Duration.ofHours(1))),
            repository);

    private final UUID customerId = UUID.randomUUID();

    private Transaction transaction() {
        return Transaction.register("BANK_A", customerId, new BigDecimal("10.00"), "PLN",
                TRANSACTION_TIME, Clock.fixed(TRANSACTION_TIME, ZoneOffset.UTC));
    }

    @Test
    void staysCleanAtExactlyTheLimit() {
        when(repository.countInWindow(any(), any(), any())).thenReturn(5L);

        assertThat(rule.evaluate(transaction())).isInstanceOf(RuleResult.Clean.class);
    }

    @Test
    void flagsTheSixthTransactionInTheWindow() {
        when(repository.countInWindow(any(), any(), any())).thenReturn(6L);

        var result = rule.evaluate(transaction());

        assertThat(result).isInstanceOf(RuleResult.Violation.class);
        assertThat(((RuleResult.Violation) result).reason()).isEqualTo(AlertReason.HIGH_FREQUENCY);
    }

    @Test
    void anchorsTheWindowToBusinessTimeOfTheTransaction() {
        when(repository.countInWindow(any(), any(), any())).thenReturn(0L);

        rule.evaluate(transaction());

        verify(repository).countInWindow(
                eq(customerId),
                eq(TRANSACTION_TIME.minus(Duration.ofHours(1))),
                eq(TRANSACTION_TIME));
    }
}
