package com.example.onboarding.domain.port.outbound;

import com.example.onboarding.domain.model.Money;

import java.util.Optional;

public interface PricingRepository {

    Optional<Money> findByCountryCode(String countryCode);
}
