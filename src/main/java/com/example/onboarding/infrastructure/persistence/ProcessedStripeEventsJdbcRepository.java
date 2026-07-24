package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.port.outbound.ProcessedStripeEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class ProcessedStripeEventsJdbcRepository implements ProcessedStripeEventRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String EXISTS = """
            SELECT EXISTS(SELECT 1 FROM processed_stripe_events WHERE event_id = :eventId)
            """;

    private static final String INSERT = """
            INSERT INTO processed_stripe_events (event_id) VALUES (:eventId)
            """;

    @Override
    public boolean isEventAlreadyProcessed(String eventId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(EXISTS, new MapSqlParameterSource("eventId", eventId), Boolean.class));
    }

    @Override
    public void markEventProcessed(String eventId) {
        jdbc.update(INSERT, new MapSqlParameterSource("eventId", eventId));
    }
}
