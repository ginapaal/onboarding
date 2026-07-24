package com.example.onboarding.domain.port.outbound;

public interface ProcessedStripeEventRepository {

    boolean isEventAlreadyProcessed(String eventId);

    void markEventProcessed(String eventId);
}
