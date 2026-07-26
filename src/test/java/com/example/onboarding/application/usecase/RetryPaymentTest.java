package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.MaxRetriesExceededException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.RetryPaymentResult;
import com.example.onboarding.domain.model.PaymentIntentId;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import com.example.onboarding.domain.port.outbound.PaymentGateway;
import com.example.onboarding.domain.port.outbound.PricingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryPaymentTest {

    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PricingRepository pricingRepository;

    @InjectMocks
    private RetryPayment retryPayment;

    private OnboardingSessionId sessionId;
    private CompanyId companyId;
    private OnboardingSession session;
    private Company company;
    private Money usdPrice;
    private CustomerReference customer;
    private PaymentIntentResult paymentIntent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retryPayment, "maxAttempts", 3);

        sessionId = OnboardingSessionId.generate();
        companyId = CompanyId.generate();
        session = OnboardingSession.create(sessionId, companyId);
        session.recordPaymentIntent(new PaymentIntentId("pi_old"));

        company = Company.register(companyId, "Acme Corp", new ContactInfo("admin@acme.com", "Jane", "Doe"));
        company.initiateActivation();
        company.activationFailed();

        usdPrice = new Money(9900, Currency.getInstance("USD"));
        customer = new CustomerReference("cus_test123");
        paymentIntent = new PaymentIntentResult(new PaymentIntentId("pi_new123"), "pi_new123_secret");
    }

    @Test
    void execute_successfulRetry_returnsNewSessionWithClientSecret() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.of(usdPrice));
        when(paymentGateway.createCustomer(any(), any())).thenReturn(customer);
        when(paymentGateway.createPaymentIntent(any(), any(), any())).thenReturn(paymentIntent);

        RetryPaymentResult result = retryPayment.execute(sessionId, "US");

        assertThat(result.clientSecret()).isEqualTo("pi_new123_secret");
        assertThat(result.pricingWarning()).isNull();
        assertThat(result.newSessionId()).isNotEqualTo(sessionId);
    }

    @Test
    void execute_createsNewSessionAndTransitionsCompanyToPendingActivation() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.of(usdPrice));
        when(paymentGateway.createCustomer(any(), any())).thenReturn(customer);
        when(paymentGateway.createPaymentIntent(any(), any(), any())).thenReturn(paymentIntent);

        retryPayment.execute(sessionId, "US");

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING_ACTIVATION);
        verify(sessionRepository).insert(any(OnboardingSession.class));
        verify(companyRepository).update(company);
    }

    @Test
    void execute_withUnknownCountry_fallsBackToUsAndReturnsWarning() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("JP")).thenReturn(Optional.empty());
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.of(usdPrice));
        when(paymentGateway.createCustomer(any(), any())).thenReturn(customer);
        when(paymentGateway.createPaymentIntent(any(), any(), any())).thenReturn(paymentIntent);

        RetryPaymentResult result = retryPayment.execute(sessionId, "JP");

        assertThat(result.pricingWarning()).isEqualTo("Could not determine pricing for your region. Defaulting to USD.");
    }

    @Test
    void execute_maxRetriesExceeded_escalatesToSupportAndRethrows() {
        ReflectionTestUtils.setField(retryPayment, "maxAttempts", 1);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> retryPayment.execute(sessionId, "US"))
                .isInstanceOf(MaxRetriesExceededException.class);

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.REQUIRES_SUPPORT);
        verify(companyRepository).update(company);
        verify(paymentGateway, never()).createPaymentIntent(any(), any(), any());
    }

    @Test
    void execute_withUnknownSession_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retryPayment.execute(sessionId, "US"))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void execute_withUnknownCompany_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retryPayment.execute(sessionId, "US"))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void execute_whenCompanyAlreadyHasStripeCustomer_skipsCreateCustomer() {
        company.assignStripeCustomer(customer);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.of(usdPrice));
        when(paymentGateway.createPaymentIntent(any(), any(), any())).thenReturn(paymentIntent);

        retryPayment.execute(sessionId, "US");

        verify(paymentGateway, never()).createCustomer(any(), any());
    }
}
