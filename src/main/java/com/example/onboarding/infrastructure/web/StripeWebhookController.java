package com.example.onboarding.infrastructure.web;

import com.example.onboarding.domain.model.PaymentEvent;
import com.example.onboarding.domain.model.PaymentEventType;
import com.example.onboarding.domain.model.PaymentIntentId;
import com.example.onboarding.domain.port.inbound.HandlePaymentEventUseCase;
import com.example.onboarding.infrastructure.web.port.StripeWebhookPort;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
public class StripeWebhookController implements StripeWebhookPort {

    private static final Map<String, PaymentEventType> STRIPE_EVENT_TYPES = Map.of(
            "payment_intent.succeeded",       PaymentEventType.PAYMENT_SUCCEEDED,
            "payment_intent.payment_failed",  PaymentEventType.PAYMENT_FAILED,
            "payment_intent.processing",      PaymentEventType.PAYMENT_PROCESSING,
            "payment_intent.canceled",        PaymentEventType.PAYMENT_CANCELED,
            "payment_intent.requires_action", PaymentEventType.ACTION_REQUIRED
    );

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

        Optional<PaymentEventType> eventType = Optional.ofNullable(STRIPE_EVENT_TYPES.get(stripeEvent.getType()));
        if (eventType.isEmpty()) {
            log.debug("Ignoring unhandled Stripe event type: {}", stripeEvent.getType());
            return ResponseEntity.ok().build();
        }

        PaymentIntent paymentIntent;
        try {
            StripeObject deserialized = stripeEvent.getDataObjectDeserializer().deserializeUnsafe();
            if (!(deserialized instanceof PaymentIntent pi)) {
                log.error("Unexpected or null Stripe event data for event {}", stripeEvent.getId());
                return ResponseEntity.internalServerError().build();
            }
            paymentIntent = pi;
        } catch (EventDataObjectDeserializationException e) {
            log.error("Could not deserialize Stripe event {}", stripeEvent.getId(), e);
            return ResponseEntity.internalServerError().build();
        }

        handlePaymentEvent.execute(new PaymentEvent(
                stripeEvent.getId(),
                eventType.get(),
                new PaymentIntentId(paymentIntent.getId())
        ));

        return ResponseEntity.ok().build();
    }
}
