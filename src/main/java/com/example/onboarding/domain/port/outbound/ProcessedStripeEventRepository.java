package com.example.onboarding.domain.port.outbound;

public interface ProcessedStripeEventRepository {

    /**
     * Atomically records an event as processed.
     *
     * @return true if the event was newly inserted (first delivery),
     *         false if it was already present (duplicate delivery)
     */
    boolean tryMarkEventProcessed(String eventId);
}
