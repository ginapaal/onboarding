package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.PaymentGateway;
import com.example.onboarding.domain.port.outbound.PricingRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiatePaymentTest {

    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PricingRepository pricingRepository;

    @InjectMocks
    private InitiatePayment initiatePayment;

    private OnboardingSessionId sessionId;
    private CompanyId companyId;
    private OnboardingSession session;
    private Company company;
    private Money usdPrice;
    private CustomerReference customer;
    private PaymentIntentResult paymentIntent;

    @BeforeEach
    void setUp() {
        sessionId = OnboardingSessionId.generate();
        companyId = CompanyId.generate();
        session = OnboardingSession.create(sessionId, companyId);
        company = Company.register(companyId, "Acme Corp", new ContactInfo("admin@acme.com", "Jane", "Doe"));
        usdPrice = new Money(9900, Currency.getInstance("USD"));
        customer = new CustomerReference("cus_test123");
        paymentIntent = new PaymentIntentResult(new StripePaymentIntentId("pi_test123"), "pi_test123_secret");
    }

    @Test
    void execute_withKnownCountry_chargesRegionalPriceAndReturnsNoWarning() {
        Money gbpPrice = new Money(7900, Currency.getInstance("GBP"));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("GB")).thenReturn(Optional.of(gbpPrice));
        when(paymentGateway.createCustomer(any(), any())).thenReturn(customer);
        when(paymentGateway.createPaymentIntent(customer, gbpPrice)).thenReturn(paymentIntent);

        InitiatePaymentResult result = initiatePayment.execute(sessionId, "GB");

        assertThat(result.clientSecret()).isEqualTo("pi_test123_secret");
        assertThat(result.pricingWarning()).isNull();
        verify(paymentGateway).createPaymentIntent(customer, gbpPrice);
    }

    @Test
    void execute_withUnknownCountry_fallsBackToUsAndReturnsWarning() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("JP")).thenReturn(Optional.empty());
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.of(usdPrice));
        when(paymentGateway.createCustomer(any(), any())).thenReturn(customer);
        when(paymentGateway.createPaymentIntent(customer, usdPrice)).thenReturn(paymentIntent);

        InitiatePaymentResult result = initiatePayment.execute(sessionId, "JP");

        assertThat(result.pricingWarning()).isEqualTo("Could not determine pricing for your region. Defaulting to USD.");
        verify(paymentGateway).createPaymentIntent(customer, usdPrice);
    }

    @Test
    void execute_withUnknownCountryAndMissingUsPricing_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("JP")).thenReturn(Optional.empty());
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initiatePayment.execute(sessionId, "JP"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("US pricing not configured");
    }

    @Test
    void execute_withUnknownSession_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initiatePayment.execute(sessionId, "US"))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void execute_withUnknownCompany_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initiatePayment.execute(sessionId, "US"))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void execute_transitionsCompanyToPendingActivationAndRecordsPaymentIntent() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(pricingRepository.findByCountryCode("US")).thenReturn(Optional.of(usdPrice));
        when(paymentGateway.createCustomer(any(), any())).thenReturn(customer);
        when(paymentGateway.createPaymentIntent(any(), any())).thenReturn(paymentIntent);

        initiatePayment.execute(sessionId, "US");

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING_ACTIVATION);
        assertThat(session.getPaymentIntentId()).isEqualTo(new StripePaymentIntentId("pi_test123"));
        assertThat(session.getClientSecret()).isEqualTo("pi_test123_secret");
        verify(sessionRepository).update(session);
        verify(companyRepository).update(company);
    }
}
