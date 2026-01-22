package com.example.price.service.impl;

import com.example.price.domain.PriceRecord;
import com.example.price.service.PriceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class LocalPriceStoreServiceTest {
    private final PriceService service = new LocalPriceStoreService();

    @Test
    void priceIsNotVisibleUntilBatchIsFinalized() {
        UUID batchId = service.startBatch();

        service.uploadPrices(batchId, List.of(
                new PriceRecord(
                        "INFY",
                        Instant.parse("2024-06-01T09:15:00Z"),
                        Map.of("rate", 1.0845)
                )
        ));

        // Before completion, consumers must not see partial data
        Assertions.assertTrue(service.getLastPrice("INFY").isEmpty());

        service.completeBatch(batchId);

        Optional<PriceRecord> result = service.getLastPrice("INFY");
        Assertions.assertTrue(result.isPresent(), "Price should be available after batch completion");
    }

    @Test
    void cancelledBatchLeavesNoResidualData() {
        UUID batchId = service.startBatch();

        service.uploadPrices(batchId, List.of(
                new PriceRecord(
                        "HCL",
                        Instant.parse("2024-06-01T10:00:00Z"),
                        Map.of("rate", 1.2721)
                )
        ));

        service.cancelBatch(batchId);

        // Data from a cancelled batch must never be published
        Assertions.assertTrue(service.getLastPrice("HCL").isEmpty());
    }

    @Test
    void mostRecentTimestampWinsWhenMultiplePricesAreProvided() {
        UUID batchId = service.startBatch();

        PriceRecord morningPrice = new PriceRecord(
                "RELIANCE",
                Instant.parse("2024-06-01T08:30:00Z"),
                Map.of("price", 64250.75)
        );

        PriceRecord afternoonPrice = new PriceRecord(
                "RELIANCE",
                Instant.parse("2024-06-01T14:45:00Z"),
                Map.of("price", 65510.20)
        );

        service.uploadPrices(batchId, List.of(morningPrice, afternoonPrice));
        service.completeBatch(batchId);

        PriceRecord result = service.getLastPrice("RELIANCE").orElseThrow();
        Assertions.assertEquals(65510.20, result.payload().get("price"));
    }

    @Test
    void uploadingPricesForNonExistingBatchFailsImmediately() {
        UUID randomBatchId = UUID.randomUUID();

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.uploadPrices(
                        randomBatchId,
                        List.of(new PriceRecord(
                                "RELIANCE",
                                Instant.parse("2024-06-01T11:00:00Z"),
                                Map.of("price", 3420.10)
                        ))
                )
        );

        Assertions.assertTrue(
                exception.getMessage().toLowerCase().contains("batch"),
                "Failure reason should clearly mention batch state"
        );
    }

    @Test
    void uploadChunkExceedingMaximumSizeIsRejected() {
        UUID batchId = service.startBatch();

        List<PriceRecord> oversizedChunk =
                java.util.stream.IntStream.range(0, 1001)
                        .mapToObj(i -> new PriceRecord(
                                "SYM" + i,
                                Instant.parse("2024-06-01T07:00:00Z"),
                                Map.of("value", i * 10.0)
                        ))
                        .toList();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadPrices(batchId, oversizedChunk),
                "Service should reject chunks larger than the allowed limit"
        );
    }

}
