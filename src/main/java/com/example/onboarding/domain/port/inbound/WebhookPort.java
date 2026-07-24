package com.example.onboarding.domain.port.inbound;

public interface WebhookPort {

    void handleStripeEvent(String rawPayload, String stripeSignature);
}
