package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.model.StripeWebhookEvent;
import com.example.onboarding.domain.model.StripeWebhookEventType;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import com.example.onboarding.domain.port.outbound.ProcessedStripeEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlePaymentEventTest {

    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ProcessedStripeEventRepository processedEventRepository;

    @InjectMocks
    private HandlePaymentEvent handlePaymentEvent;

    private StripePaymentIntentId paymentIntentId;
    private OnboardingSession session;
    private Company company;

    @BeforeEach
    void setUp() {
        CompanyId companyId = CompanyId.generate();
        paymentIntentId = new StripePaymentIntentId("pi_test123");

        OnboardingSessionId sessionId = OnboardingSessionId.generate();
        session = OnboardingSession.create(sessionId, companyId);
        session.recordPaymentIntent(paymentIntentId);

        company = Company.register(companyId, "Acme Corp", new ContactInfo("admin@acme.com", "Jane", "Doe"));
        company.initiateActivation();

        lenient().when(sessionRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.of(session));
        lenient().when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
    }

    @Test
    void execute_paymentSucceeded_activatesCompany() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_1")).thenReturn(false);

        handlePaymentEvent.execute(event("evt_1", StripeWebhookEventType.PAYMENT_SUCCEEDED));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        verify(companyRepository).update(company);
        verify(processedEventRepository).markEventProcessed("evt_1");
    }

    @Test
    void execute_paymentFailed_setsActivationFailed() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_2")).thenReturn(false);

        handlePaymentEvent.execute(event("evt_2", StripeWebhookEventType.PAYMENT_FAILED));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_FAILED);
        verify(companyRepository).update(company);
    }

    @Test
    void execute_paymentProcessing_setsActivationProcessing() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_3")).thenReturn(false);

        handlePaymentEvent.execute(event("evt_3", StripeWebhookEventType.PAYMENT_PROCESSING));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_PROCESSING);
        verify(companyRepository).update(company);
    }

    @Test
    void execute_paymentCanceled_fromPendingActivation_setsActivationCanceled() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_4")).thenReturn(false);

        handlePaymentEvent.execute(event("evt_4", StripeWebhookEventType.PAYMENT_CANCELED));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_CANCELED);
        verify(companyRepository).update(company);
    }

    @Test
    void execute_actionRequired_setsActionRequired() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_5")).thenReturn(false);

        handlePaymentEvent.execute(event("evt_5", StripeWebhookEventType.ACTION_REQUIRED));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTION_REQUIRED);
        verify(companyRepository).update(company);
    }

    @Test
    void execute_alreadyProcessed_skipsProcessing() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_dup")).thenReturn(true);

        handlePaymentEvent.execute(event("evt_dup", StripeWebhookEventType.PAYMENT_SUCCEEDED));

        verify(companyRepository, never()).update(company);
        verify(processedEventRepository, never()).markEventProcessed("evt_dup");
    }

    @Test
    void execute_sessionNotFound_throws() {
        when(processedEventRepository.isEventAlreadyProcessed("evt_6")).thenReturn(false);
        when(sessionRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handlePaymentEvent.execute(event("evt_6", StripeWebhookEventType.PAYMENT_SUCCEEDED)))
                .isInstanceOf(SessionNotFoundException.class);
    }

    private StripeWebhookEvent event(String eventId, StripeWebhookEventType type) {
        return new StripeWebhookEvent(eventId, type, paymentIntentId);
    }
}
