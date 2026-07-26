package com.example.onboarding.domain.exception;

import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentId;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(OnboardingSessionId id) {
        super("Onboarding session not found: " + id.value());
    }

    public SessionNotFoundException(PaymentIntentId paymentIntentId) {
        super("Onboarding session not found for payment intent: " + paymentIntentId.value());
    }
}
