package com.example.onboarding.domain.port.outbound;

import com.example.onboarding.domain.model.NotificationOutboxMessage;

public interface NotificationPort {
     void sendNotificationEvent(NotificationOutboxMessage message);
}
