package com.example.onboarding.domain.port;

public interface WebhookPort {

    void handleStripeEvent(String rawPayload, String stripeSignature);
}
