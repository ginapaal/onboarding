package com.example.onboarding.domain.model;

import java.util.Currency;

public record Money(long amountInMinorUnits, Currency currency) {}
