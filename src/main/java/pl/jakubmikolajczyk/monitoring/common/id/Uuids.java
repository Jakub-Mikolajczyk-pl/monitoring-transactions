package pl.jakubmikolajczyk.monitoring.common.id;

import java.security.SecureRandom;
import java.util.UUID;

/// Generates RFC 9562 UUIDv7 identifiers: a 48-bit Unix timestamp (milliseconds)
/// followed by random bits, with standard version (7) and variant (IETF) fields.
///
/// Time-ordered identifiers keep B-tree index inserts append-friendly while staying
/// globally unique, and the application stays independent from database sequences.
/// See ADR-0011. The JDK (as of 25) only ships UUIDv4 via `UUID.randomUUID()`.
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {
    }

    public static UUID v7() {
        long timestampMs = System.currentTimeMillis();
        long mostSignificant = (timestampMs << 16)              // 48-bit ms timestamp
                | 0x7000L                                       // version: 7
                | (RANDOM.nextLong() & 0x0FFFL);                // 12 random bits
        long leastSignificant = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL)
                | 0x8000000000000000L;                          // variant: IETF (10xx...)
        return new UUID(mostSignificant, leastSignificant);
    }
}
