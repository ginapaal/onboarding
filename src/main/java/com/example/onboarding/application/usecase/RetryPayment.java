package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.MaxRetriesExceededException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.RetryPaymentResult;
import com.example.onboarding.domain.port.inbound.RetryPaymentUseCase;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import com.example.onboarding.domain.port.outbound.PaymentGateway;
import com.example.onboarding.domain.port.outbound.PricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RetryPayment implements RetryPaymentUseCase {

    private static final String FALLBACK_COUNTRY = "US";
    private static final String FALLBACK_WARNING =
            "Could not determine pricing for your region. Defaulting to USD.";

    private final OnboardingSessionRepository sessionRepository;
    private final CompanyRepository companyRepository;
    private final PaymentGateway paymentGateway;
    private final PricingRepository pricingRepository;

    @Value("${onboarding.payment.max-attempts}")
    private int maxAttempts;

    @Override
    @Transactional(noRollbackFor = MaxRetriesExceededException.class)
    public RetryPaymentResult execute(OnboardingSessionId currentSessionId, String ipCountry) {
        OnboardingSession currentSession = sessionRepository.findById(currentSessionId)
                .orElseThrow(() -> new SessionNotFoundException(currentSessionId));

        Company company = companyRepository.findById(currentSession.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(currentSession.getCompanyId()));

        try {
            company.retryActivation(maxAttempts);
        } catch (MaxRetriesExceededException e) {
            company.escalateToSupport();
            companyRepository.update(company);
            throw e;
        }

        Optional<Money> regionPrice = pricingRepository.findByCountryCode(ipCountry);
        Money price = regionPrice
                .or(() -> pricingRepository.findByCountryCode(FALLBACK_COUNTRY))
                .orElseThrow(() -> new IllegalStateException("US pricing not configured"));
        String pricingWarning = regionPrice.isEmpty() ? FALLBACK_WARNING : null;

        ensureStripeCustomerExists(company);

        OnboardingSessionId newSessionId = OnboardingSessionId.generate();
        OnboardingSession newSession = OnboardingSession.create(newSessionId, company.getId());

        PaymentIntentResult paymentIntent = paymentGateway.createPaymentIntent(
                company.getStripeCustomerReference(), price, newSessionId);

        newSession.recordPaymentIntent(paymentIntent.id());

        sessionRepository.insert(newSession);
        companyRepository.update(company);

        return new RetryPaymentResult(newSessionId, paymentIntent.clientSecret(), pricingWarning);
    }

    private void ensureStripeCustomerExists(Company company) {
        if (company.getStripeCustomerReference() == null) {
            company.assignStripeCustomer(paymentGateway.createCustomer(company.getAdminContact(), company.getId()));
        }
    }
}
