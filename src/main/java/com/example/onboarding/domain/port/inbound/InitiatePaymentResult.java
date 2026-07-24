package com.example.onboarding.domain.port.inbound;

public record InitiatePaymentResult(String clientSecret, String pricingWarning) {}
