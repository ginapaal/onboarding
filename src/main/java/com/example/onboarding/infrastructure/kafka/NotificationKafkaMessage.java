package com.example.onboarding.infrastructure.kafka;

public record NotificationKafkaMessage(
        String companyId,
        String adminEmail,
        String adminFirstName,
        String adminLastName,
        String notificationType,
        String body
) {}
