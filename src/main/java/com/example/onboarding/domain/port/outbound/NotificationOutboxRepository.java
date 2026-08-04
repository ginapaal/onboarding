package com.example.onboarding.domain.port.outbound;

import com.example.onboarding.domain.model.NotificationOutboxMessage;

import java.util.List;

public interface NotificationOutboxRepository {
    void saveOutboxEvent(NotificationOutboxMessage message);
    List<NotificationOutboxMessage> findUnprocessed();
    void markAsProcessed(Long id);
}
