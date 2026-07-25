package com.example.onboarding.infrastructure.web;

import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.model.StripeWebhookEvent;
import com.example.onboarding.domain.model.StripeWebhookEventType;
import com.example.onboarding.domain.port.inbound.HandlePaymentEventUseCase;
import com.example.onboarding.infrastructure.web.port.StripeWebhookPort;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@RestController
public class StripeWebhookController implements StripeWebhookPort {

    private final HandlePaymentEventUseCase handlePaymentEvent;
    private final String webhookSecret;

    public StripeWebhookController(
            HandlePaymentEventUseCase handlePaymentEvent,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.handlePaymentEvent = handlePaymentEvent;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public ResponseEntity<Void> handleWebhook(byte[] payload, String stripeSignature) {
        com.stripe.model.Event stripeEvent;
        try {
            stripeEvent = Webhook.constructEvent(
                    new String(payload, StandardCharsets.UTF_8), stripeSignature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        Optional<StripeWebhookEventType> eventType = StripeWebhookEventType.fromStripeType(stripeEvent.getType());
        if (eventType.isEmpty()) {
            log.debug("Ignoring unhandled Stripe event type: {}", stripeEvent.getType());
            return ResponseEntity.ok().build();
        }

        PaymentIntent paymentIntent;
        try {
            paymentIntent = (PaymentIntent) stripeEvent.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            throw new IllegalStateException("Could not deserialize Stripe event data", e);
        }

        handlePaymentEvent.execute(new StripeWebhookEvent(
                stripeEvent.getId(),
                eventType.get(),
                new StripePaymentIntentId(paymentIntent.getId())
        ));

        return ResponseEntity.ok().build();
    }
}
