package pl.jakubmikolajczyk.monitoring.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class UuidsTest {

    @Test
    void generatesVersion7WithIetfVariant() {
        UUID id = Uuids.v7();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2); // IETF variant per RFC 9562
    }

    @Test
    void embedsCurrentUnixTimestampInTopBits() {
        long before = System.currentTimeMillis();
        UUID id = Uuids.v7();
        long after = System.currentTimeMillis();

        long embeddedMillis = id.getMostSignificantBits() >>> 16;

        assertThat(embeddedMillis).isBetween(before, after);
    }

    @Test
    void generatedIdsAreUnique() {
        var ids = IntStream.range(0, 10_000)
                .mapToObj(i -> Uuids.v7())
                .collect(Collectors.toSet());

        assertThat(ids).hasSize(10_000);
    }
}
