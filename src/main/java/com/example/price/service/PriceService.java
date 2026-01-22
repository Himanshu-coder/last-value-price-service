package com.example.price.service;

import com.example.price.domain.PriceRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceService {
    /**
     * Starts a new batch run.
     *
     * @return unique batch identifier
     */
    UUID startBatch();

    /**
     * Uploads a chunk of price records for a given batch.
     *
     * Implementations must:
     * - Reject uploads for unknown or completed batches
     * - Accept chunks up to 1000 records
     *
     * @param batchId batch identifier
     * @param records list of price records
     */
    void uploadPrices(UUID batchId, List<PriceRecord> records);

    /**
     * Completes the batch and atomically makes all its prices visible.
     *
     * @param batchId batch identifier
     */
    void completeBatch(UUID batchId);

    /**
     * Cancels the batch and discards all uploaded data.
     *
     * @param batchId batch identifier
     */
    void cancelBatch(UUID batchId);

    /**
     * Retrieves the last known price for a given instrument.
     *
     * @param instrumentId instrument identifier
     * @return optional last price record
     */
    Optional<PriceRecord> getLastPrice(String instrumentId);

}
