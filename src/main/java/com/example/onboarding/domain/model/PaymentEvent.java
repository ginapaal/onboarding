package com.example.onboarding.domain.model;

public record PaymentEvent(
        String eventId,
        PaymentEventType type,
        PaymentIntentId paymentIntentId
) {}
