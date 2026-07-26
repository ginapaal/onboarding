package com.example.onboarding.infrastructure.persistence;

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

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ProcessedStripeEventsJdbcRepository.class, FlywayAutoConfiguration.class})
class ProcessedStripeEventsJdbcRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProcessedStripeEventsJdbcRepository repository;

    @Test
    void recordIfNew_newEvent_returnsTrue() {
        assertThat(repository.recordIfNew("evt_new")).isTrue();
    }

    @Test
    void recordIfNew_duplicateEvent_returnsFalse() {
        repository.recordIfNew("evt_dup");

        assertThat(repository.recordIfNew("evt_dup")).isFalse();
    }

    @Test
    void recordIfNew_doesNotAffectOtherEvents() {
        repository.recordIfNew("evt_one");

        assertThat(repository.recordIfNew("evt_two")).isTrue();
    }
}
