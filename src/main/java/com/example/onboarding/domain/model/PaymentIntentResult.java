package com.example.onboarding.domain.model;

public record PaymentIntentResult(StripePaymentIntentId id, String clientSecret) {}
