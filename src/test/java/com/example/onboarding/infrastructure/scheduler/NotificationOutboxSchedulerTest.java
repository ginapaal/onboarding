package com.example.onboarding.infrastructure.scheduler;

import com.example.onboarding.domain.model.ChannelType;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.NotificationOutboxMessage;
import com.example.onboarding.domain.model.NotificationType;
import com.example.onboarding.domain.port.outbound.NotificationOutboxRepository;
import com.example.onboarding.domain.port.outbound.NotificationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxSchedulerTest {

    @Mock private NotificationOutboxRepository notificationOutboxRepository;
    @Mock private NotificationPort notificationPort;

    @InjectMocks
    private NotificationOutboxScheduler scheduler;

    @Test
    void processOutbox_sendsEachMessageAndMarksProcessed() {
        NotificationOutboxMessage msg1 = message(1L, NotificationType.REGISTERED);
        NotificationOutboxMessage msg2 = message(2L, NotificationType.ACTIVATED);
        when(notificationOutboxRepository.findUnprocessed()).thenReturn(List.of(msg1, msg2));

        scheduler.processOutbox();

        verify(notificationPort).sendNotificationEvent(msg1);
        verify(notificationOutboxRepository).markAsProcessed(1L);
        verify(notificationPort).sendNotificationEvent(msg2);
        verify(notificationOutboxRepository).markAsProcessed(2L);
    }

    @Test
    void processOutbox_noMessages_doesNothing() {
        when(notificationOutboxRepository.findUnprocessed()).thenReturn(List.of());

        scheduler.processOutbox();

        verify(notificationPort, never()).sendNotificationEvent(any());
        verify(notificationOutboxRepository, never()).markAsProcessed(any());
    }

    @Test
    void processOutbox_sendFailure_continuesWithRemainingMessages() {
        NotificationOutboxMessage msg1 = message(1L, NotificationType.REGISTERED);
        NotificationOutboxMessage msg2 = message(2L, NotificationType.ACTIVATED);
        when(notificationOutboxRepository.findUnprocessed()).thenReturn(List.of(msg1, msg2));
        doThrow(new RuntimeException("Kafka unavailable")).when(notificationPort).sendNotificationEvent(msg1);

        scheduler.processOutbox();

        verify(notificationOutboxRepository, never()).markAsProcessed(1L);
        verify(notificationPort).sendNotificationEvent(msg2);
        verify(notificationOutboxRepository).markAsProcessed(2L);
    }

    private NotificationOutboxMessage message(Long id, NotificationType notificationType) {
        return new NotificationOutboxMessage(id, CompanyId.generate(), "admin@acme.com", "Jane", "Doe", notificationType, ChannelType.EMAIL, false);
    }
}
