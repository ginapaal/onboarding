package com.example.onboarding.domain.model;

import java.util.UUID;

public record SessionId(UUID value) {

    public static SessionId generate() {
        return new SessionId(UUID.randomUUID());
    }
}
