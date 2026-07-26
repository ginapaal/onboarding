package com.example.onboarding.domain.port.outbound;

public interface ProcessedStripeEventRepository {

    /**
     * Records the event as processed if not already seen.
     *
     * @return true if this is the first delivery, false if it is a duplicate
     */
    boolean recordIfNew(String eventId);
}
