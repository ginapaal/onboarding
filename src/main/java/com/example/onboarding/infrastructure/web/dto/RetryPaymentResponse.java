package com.example.onboarding.infrastructure.web.dto;

import java.util.UUID;

public record RetryPaymentResponse(UUID newSessionId, String clientSecret, String pricingWarning) {}
