package com.example.onboarding.domain.exception;

import com.example.onboarding.domain.model.SessionId;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(SessionId id) {
        super("Onboarding session not found: " + id.value());
    }
}
