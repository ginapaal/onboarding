package com.example.onboarding.infrastructure.web;

import com.example.onboarding.domain.port.WebhookPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class WebhookController implements WebhookPort {

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestHeader("Stripe-Signature") String stripeSignature,
            @RequestBody byte[] rawPayload) {
        handleStripeEvent(new String(rawPayload), stripeSignature);
        return ResponseEntity.ok().build();
    }

    // --- WebhookPort ---

    @Override
    public void handleStripeEvent(String rawPayload, String stripeSignature) {
        // stub — signature verification and event routing to be implemented
    }
}
