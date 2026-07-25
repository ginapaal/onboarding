package com.example.onboarding.domain.model;

public record RetryPaymentResult(OnboardingSessionId newSessionId, String clientSecret, String pricingWarning) {}
