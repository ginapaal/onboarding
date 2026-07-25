package com.example.onboarding.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RetryPaymentRequest(@NotBlank String ipCountry) {}
