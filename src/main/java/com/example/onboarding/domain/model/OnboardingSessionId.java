package com.example.onboarding.domain.model;

import java.util.UUID;

public record OnboardingSessionId(UUID value) {

    public static OnboardingSessionId generate() {
        return new OnboardingSessionId(UUID.randomUUID());
    }
}
