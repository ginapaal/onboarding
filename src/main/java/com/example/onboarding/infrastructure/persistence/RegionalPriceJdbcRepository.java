package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.port.outbound.PricingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RegionalPriceJdbcRepository implements PricingRepository {

    @Override
    public Optional<Money> findByCountryCode(String countryCode) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
