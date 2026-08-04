package com.example.onboarding.domain.model;

public record NotificationOutboxMessage(Long id,
                                        CompanyId companyId,
                                        String adminEmail,
                                        String adminFirstName,
                                        String adminLastName,
                                        NotificationType notificationType,
                                        ChannelType type,
                                        boolean processed) {

}
