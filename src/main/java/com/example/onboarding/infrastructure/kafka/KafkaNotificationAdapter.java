package com.example.onboarding.infrastructure.kafka;

import com.example.onboarding.domain.model.NotificationOutboxMessage;
import com.example.onboarding.domain.model.NotificationType;
import com.example.onboarding.domain.port.outbound.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class KafkaNotificationAdapter implements NotificationPort {

    private static final String TOPIC = "notification.messages";

    private final KafkaTemplate<String, NotificationKafkaMessage> kafkaTemplate;

    @Override
    public void sendNotificationEvent(NotificationOutboxMessage message) {
        String body = resolveBody(message.notificationType());
        NotificationKafkaMessage kafkaMessage = new NotificationKafkaMessage(
                message.companyId().value().toString(),
                message.adminEmail(),
                message.adminFirstName(),
                message.adminLastName(),
                message.notificationType().name(),
                body
        );
        try {
            kafkaTemplate.send(TOPIC, message.companyId().value().toString(), kafkaMessage).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending notification to Kafka", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to send notification to Kafka", e.getCause());
        }
    }

    private String resolveBody(NotificationType type) {
        return switch (type) {
            case REGISTERED -> "Welcome! Your company has been successfully registered. Our team will review your details shortly.";
            case ACTIVATED -> "Congratulations! Your company account is now active. You can start using our platform.";
        };
    }
}
