package com.example.onboarding.infrastructure.web;

import com.example.onboarding.domain.model.PaymentEvent;
import com.example.onboarding.domain.model.PaymentEventType;
import com.example.onboarding.domain.port.inbound.HandlePaymentEventUseCase;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private HandlePaymentEventUseCase handlePaymentEvent;

    private StripeWebhookController controller;

    private static final byte[] PAYLOAD = "{}".getBytes(StandardCharsets.UTF_8);
    private static final String SIGNATURE = "t=123,v1=abc";

    @BeforeEach
    void setUp() {
        controller = new StripeWebhookController(handlePaymentEvent, "whsec_test");
    }

    @Test
    void handleWebhook_invalidSignature_returns400() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException("Bad signature", SIGNATURE));

            ResponseEntity<Void> response = controller.handleWebhook(PAYLOAD, SIGNATURE);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            verifyNoInteractions(handlePaymentEvent);
        }
    }

    @Test
    void handleWebhook_unknownEventType_returns200WithoutCallingUseCase() {
        com.stripe.model.Event event = mockEventOfType("customer.created");

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            ResponseEntity<Void> response = controller.handleWebhook(PAYLOAD, SIGNATURE);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verifyNoInteractions(handlePaymentEvent);
        }
    }

    @Test
    void handleWebhook_validPaymentSucceededEvent_invokesUseCaseWithCorrectEvent() throws Exception {
        com.stripe.model.Event event = mockPaymentIntentEvent("payment_intent.succeeded", "pi_test123");

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            ResponseEntity<Void> response = controller.handleWebhook(PAYLOAD, SIGNATURE);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(handlePaymentEvent).execute(argThat((PaymentEvent e) ->
                    e.type() == PaymentEventType.PAYMENT_SUCCEEDED &&
                    e.paymentIntentId().value().equals("pi_test123")));
        }
    }

    private com.stripe.model.Event mockEventOfType(String type) {
        com.stripe.model.Event event = mock(com.stripe.model.Event.class);
        when(event.getType()).thenReturn(type);
        return event;
    }

    private com.stripe.model.Event mockPaymentIntentEvent(String type, String paymentIntentId)
            throws EventDataObjectDeserializationException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn(paymentIntentId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn(paymentIntent);

        com.stripe.model.Event event = mock(com.stripe.model.Event.class);
        when(event.getType()).thenReturn(type);
        when(event.getId()).thenReturn("evt_test_" + paymentIntentId);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        return event;
    }
}
