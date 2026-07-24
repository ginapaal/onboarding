package com.example.onboarding.infrastructure.web.dto;

import jakarta.validation.constraints.Pattern;

public record InitiatePaymentRequest(
        @Pattern(regexp = "^[A-Z]{2}$") String ipCountry
) {}
