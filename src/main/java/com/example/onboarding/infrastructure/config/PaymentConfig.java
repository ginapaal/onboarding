package com.example.onboarding.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.payment")
public record PaymentConfig(long amountInMinorUnits, String currency, int maxRetries) {}
