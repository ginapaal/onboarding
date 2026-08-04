package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.ChannelType;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.NotificationOutboxMessage;
import com.example.onboarding.domain.model.NotificationType;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({NotificationOutboxJdbcRepository.class, FlywayAutoConfiguration.class})
class NotificationOutboxJdbcRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NotificationOutboxJdbcRepository repository;

    @Test
    void saveOutboxEvent_persistsRecord() {
        NotificationOutboxMessage message = message(NotificationType.REGISTERED);

        repository.saveOutboxEvent(message);

        List<NotificationOutboxMessage> unprocessed = repository.findUnprocessed();
        assertThat(unprocessed).hasSize(1);

        NotificationOutboxMessage saved = unprocessed.getFirst();
        assertThat(saved.adminEmail()).isEqualTo("admin@acme.com");
        assertThat(saved.adminFirstName()).isEqualTo("Jane");
        assertThat(saved.adminLastName()).isEqualTo("Doe");
        assertThat(saved.notificationType()).isEqualTo(NotificationType.REGISTERED);
        assertThat(saved.type()).isEqualTo(ChannelType.EMAIL);
        assertThat(saved.processed()).isFalse();
    }

    @Test
    void findUnprocessed_returnsOnlyUnprocessedRows() {
        repository.saveOutboxEvent(message(NotificationType.REGISTERED));
        repository.saveOutboxEvent(message(NotificationType.ACTIVATED));

        List<NotificationOutboxMessage> unprocessed = repository.findUnprocessed();
        assertThat(unprocessed).hasSize(2);

        repository.markAsProcessed(unprocessed.getFirst().id());

        assertThat(repository.findUnprocessed()).hasSize(1);
    }

    @Test
    void markAsProcessed_updatesCorrectRow() {
        repository.saveOutboxEvent(message(NotificationType.REGISTERED));
        repository.saveOutboxEvent(message(NotificationType.ACTIVATED));

        List<NotificationOutboxMessage> unprocessed = repository.findUnprocessed();
        Long targetId = unprocessed.getFirst().id();
        repository.markAsProcessed(targetId);

        List<NotificationOutboxMessage> remaining = repository.findUnprocessed();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().id()).isNotEqualTo(targetId);
    }

    private NotificationOutboxMessage message(NotificationType notificationType) {
        return new NotificationOutboxMessage(null, CompanyId.generate(), "admin@acme.com", "Jane", "Doe", notificationType, ChannelType.EMAIL, false);
    }
}
