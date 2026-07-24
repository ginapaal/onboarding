package com.example.onboarding.infrastructure.web.dto;

public record InitiatePaymentResponse(String clientSecret, String pricingWarning) {}
