package com.example.price.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class PriceRecordTest {
    @Test
    void shouldCreateValidPriceRecord() {
        PriceRecord record = new PriceRecord(
                "AAA",
                Instant.now(),
                Map.of("price", 150.25)
        );

        assertEquals("AAA", record.id());
        assertNotNull(record.timestamp());
        assertEquals(150.25, record.payload().get("price"));
    }

    @Test
    void shouldFailWhenIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                new PriceRecord(null, Instant.now(), Map.of())
        );
    }

}
