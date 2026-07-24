package com.example.onboarding.domain.model;

import java.util.Currency;

public record Money(long amountInCents, Currency currency) {}
