package com.example.price.service.impl;

import com.example.price.domain.PriceRecord;
import com.example.price.service.PriceService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory implementation of PriceService.
 *
 * This implementation keeps batch uploads isolated until explicitly completed.
 * Once a batch is completed, its data becomes visible to readers atomically.
 *
 * We use ConcurrentHashMap for lock-free reading.
 */

public class LocalPriceStoreService implements PriceService {
    /**
     * Holds the latest visible price per instrument.
     * ConcurrentHashMap allows lock-free reads for consumers.
     */
    private final Map<String, PriceRecord> livePriceIndex = new ConcurrentHashMap<>();

    /**
     * Temporary storage for batches that are currently being uploaded.
     * Each batch maintains its own isolated buffer.
     */
    private final Map<UUID, Map<String, PriceRecord>> openBatchBuffer = new ConcurrentHashMap<>();

    @Override
    public UUID startBatch() {
        UUID batchId = UUID.randomUUID();
        openBatchBuffer.put(batchId, new HashMap<>());
        return batchId;
    }

    @Override
    public void uploadPrices(UUID batchId, List<PriceRecord> records) {
        if (records.size() > 1000) {
            throw new IllegalArgumentException("Maximum chunk size of 1000 records exceeded");
        }

        Map<String, PriceRecord> bufferedPrices = openBatchBuffer.get(batchId);
        if (bufferedPrices == null) {
            throw new IllegalStateException("Batch not found or already closed: " + batchId);
        }

        for (PriceRecord record : records) {
            bufferedPrices.merge(
                    record.id(),
                    record,
                    this::pickMostRecent
            );
        }
    }

    /**
     * Completes the batch and promotes its data to the live price index.
     * This method is synchronized to guarantee atomic visibility.
     */
    @Override
    public synchronized void completeBatch(UUID batchId) {
        Map<String, PriceRecord> bufferedPrices = openBatchBuffer.remove(batchId);
        if (bufferedPrices == null) {
            throw new IllegalStateException("Batch not found or already finalized: " + batchId);
        }

        bufferedPrices.values().forEach(record ->
                livePriceIndex.merge(
                        record.id(),
                        record,
                        this::pickMostRecent
                )
        );
    }

    /**
     * Cancels a batch and discards all uploaded data for it.
     */
    @Override
    public void cancelBatch(UUID batchId) {
        openBatchBuffer.remove(batchId);
    }

    /**
     * Returns the last known price for the given instrument, if available.
     */
    @Override
    public Optional<PriceRecord> getLastPrice(String instrumentId) {
        return Optional.ofNullable(livePriceIndex.get(instrumentId));
    }

    /**
     * Chooses the price record with the most recent timestamp.
     */
    private PriceRecord pickMostRecent(PriceRecord existing, PriceRecord candidate) {
        return candidate.timestamp().isAfter(existing.timestamp())
                ? candidate
                : existing;
    }
}
