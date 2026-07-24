package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.port.outbound.PricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RegionalPriceJdbcRepository implements PricingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String FIND_BY_COUNTRY_CODE = """
            SELECT amount_in_minor_units, currency
            FROM regional_prices
            WHERE country_code = :countryCode
            """;

    @Override
    public Optional<Money> findByCountryCode(String countryCode) {
        return jdbc.query(FIND_BY_COUNTRY_CODE,
                new MapSqlParameterSource("countryCode", countryCode),
                (rs, rowNum) -> new Money(
                        rs.getLong("amount_in_minor_units"),
                        Currency.getInstance(rs.getString("currency"))
                ))
                .stream().findFirst();
    }
}
