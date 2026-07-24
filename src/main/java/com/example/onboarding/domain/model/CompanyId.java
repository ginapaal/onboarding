package com.example.onboarding.domain.model;

import java.util.UUID;

public record CompanyId(UUID value) {

    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID());
    }
}
