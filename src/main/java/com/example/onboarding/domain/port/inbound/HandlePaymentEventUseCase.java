package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.StripeWebhookEvent;

public interface HandlePaymentEventUseCase {
    void execute(StripeWebhookEvent event);
}
