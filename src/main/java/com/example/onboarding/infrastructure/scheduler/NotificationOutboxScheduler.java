package com.example.onboarding.infrastructure.scheduler;

import com.example.onboarding.domain.model.NotificationOutboxMessage;
import com.example.onboarding.domain.port.outbound.NotificationOutboxRepository;
import com.example.onboarding.domain.port.outbound.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxScheduler {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationPort notificationPort;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void processOutbox() {
        List<NotificationOutboxMessage> pending = notificationOutboxRepository.findUnprocessed();
        for (NotificationOutboxMessage message : pending) {
            try {
                notificationOutboxRepository.markAsProcessed(message.id());
                notificationPort.sendNotificationEvent(message);
            } catch (Exception e) {
                log.error("Failed to process outbox message id={}", message.id(), e);
            }
        }
    }
}
