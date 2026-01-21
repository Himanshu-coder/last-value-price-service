package com.example.price.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Price Record
 *
 * Java 17 record is used intentionally because this class:
 * - Requires immutability which will ensure thread safety.
 *
 * Instant is used instead of LocalDateTime to avoid timezone ambiguity.
 * Payload is represented as a Map to support different pricing schemas.
 */
public record PriceRecord(
        String id,
        Instant timestamp,
        Map<String, Object> payload
) {
    public PriceRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }
}
