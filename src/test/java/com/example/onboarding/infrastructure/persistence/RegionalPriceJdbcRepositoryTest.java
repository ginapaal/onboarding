package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({RegionalPriceJdbcRepository.class, FlywayAutoConfiguration.class})
class RegionalPriceJdbcRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RegionalPriceJdbcRepository repository;

    @Test
    void findByCountryCode_returnsSeededPrice() {
        Optional<Money> result = repository.findByCountryCode("GB");

        assertThat(result).isPresent();
        assertThat(result.get().amountInMinorUnits()).isEqualTo(7900);
        assertThat(result.get().currency()).isEqualTo(Currency.getInstance("GBP"));
    }

    @Test
    void findByCountryCode_returnsEmptyForUnknownCountry() {
        assertThat(repository.findByCountryCode("JP")).isEmpty();
    }
}
