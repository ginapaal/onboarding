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

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO processed_stripe_events (event_id) VALUES (:eventId)
            ON CONFLICT (event_id) DO NOTHING
            """;

    @Override
    public boolean tryMarkEventProcessed(String eventId) {
        return jdbc.update(INSERT_IF_ABSENT, new MapSqlParameterSource("eventId", eventId)) > 0;
    }
}
