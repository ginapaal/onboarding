package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.ChannelType;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.NotificationOutboxMessage;
import com.example.onboarding.domain.model.NotificationType;
import com.example.onboarding.domain.port.outbound.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxJdbcRepository implements NotificationOutboxRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String INSERT = """
            INSERT INTO notification_outbox (company_id, admin_email, admin_first_name, admin_last_name, notification_type, channel_type)
            VALUES (:companyId, :adminEmail, :adminFirstName, :adminLastName, :notificationType, :channelType)
            """;

    private static final String FIND_UNPROCESSED = """
            SELECT id, company_id, admin_email, admin_first_name, admin_last_name, notification_type, channel_type, processed
            FROM notification_outbox
            WHERE processed = false
            """;

    private static final String MARK_AS_PROCESSED = """
            UPDATE notification_outbox SET processed = true WHERE id = :id
            """;

    private static final RowMapper<NotificationOutboxMessage> ROW_MAPPER = (rs, rowNum) ->
            new NotificationOutboxMessage(
                    rs.getLong("id"),
                    new CompanyId(rs.getObject("company_id", UUID.class)),
                    rs.getString("admin_email"),
                    rs.getString("admin_first_name"),
                    rs.getString("admin_last_name"),
                    NotificationType.valueOf(rs.getString("notification_type")),
                    ChannelType.valueOf(rs.getString("channel_type")),
                    rs.getBoolean("processed")
            );

    @Override
    public void saveOutboxEvent(NotificationOutboxMessage message) {
        jdbc.update(INSERT, new MapSqlParameterSource()
                .addValue("companyId", message.companyId().value().toString())
                .addValue("adminEmail", message.adminEmail())
                .addValue("adminFirstName", message.adminFirstName())
                .addValue("adminLastName", message.adminLastName())
                .addValue("notificationType", message.notificationType().name())
                .addValue("channelType", message.type().name()));
    }

    @Override
    public List<NotificationOutboxMessage> findUnprocessed() {
        return jdbc.query(FIND_UNPROCESSED, Map.of(), ROW_MAPPER);
    }

    @Override
    public void markAsProcessed(Long id) {
        jdbc.update(MARK_AS_PROCESSED, new MapSqlParameterSource("id", id));
    }
}
