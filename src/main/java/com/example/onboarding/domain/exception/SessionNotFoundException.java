package com.example.onboarding.domain.exception;

import com.example.onboarding.domain.model.OnboardingSessionId;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(OnboardingSessionId id) {
        super("Onboarding session not found: " + id.value());
    }
}
