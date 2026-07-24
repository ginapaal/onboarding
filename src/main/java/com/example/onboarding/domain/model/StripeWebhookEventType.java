package com.example.onboarding.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum StripeWebhookEventType {

    PAYMENT_SUCCEEDED("payment_intent.succeeded"),
    PAYMENT_FAILED("payment_intent.payment_failed"),
    PAYMENT_PROCESSING("payment_intent.processing"),
    PAYMENT_CANCELED("payment_intent.canceled"),
    ACTION_REQUIRED("payment_intent.requires_action");

    private final String stripeType;

    StripeWebhookEventType(String stripeType) {
        this.stripeType = stripeType;
    }

    public static Optional<StripeWebhookEventType> fromStripeType(String type) {
        return Arrays.stream(values())
                .filter(e -> e.stripeType.equals(type))
                .findFirst();
    }
}
