package com.example.onboarding.domain.model;

public record StripeWebhookEvent(
        String eventId,
        StripeWebhookEventType type,
        StripePaymentIntentId paymentIntentId
) {}
