package com.example.price.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

public class PriceRecordTest {
    @Test
    void shouldCreateValidPriceRecord() {
        PriceRecord record = new PriceRecord(
                "HCL",
                Instant.now(),
                Map.of("price", 150.25)
        );

        Assertions.assertEquals("HCL", record.id());
        Assertions.assertNotNull(record.timestamp());
        Assertions.assertEquals(150.25, record.payload().get("price"));
    }

    @Test
    void shouldFailWhenIdIsNull() {
        Assertions.assertThrows(NullPointerException.class, () ->
                new PriceRecord(null, Instant.now(), Map.of())
        );
    }

}
