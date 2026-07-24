package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.PaymentGateway;
import com.example.onboarding.domain.port.outbound.PricingRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InitiatePayment {

    private static final String FALLBACK_COUNTRY = "US";
    private static final String FALLBACK_WARNING =
            "Could not determine pricing for your region. Defaulting to USD.";

    private final OnboardingSessionRepository sessionRepository;
    private final CompanyRepository companyRepository;
    private final PaymentGateway paymentGateway;
    private final PricingRepository pricingRepository;

    @Transactional
    public InitiatePaymentResult execute(OnboardingSessionId sessionId, String ipCountry) {
        OnboardingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        Company company = companyRepository.findById(session.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(session.getCompanyId()));

        Optional<Money> regionPrice = pricingRepository.findByCountryCode(ipCountry);
        Money price = regionPrice
                .or(() -> pricingRepository.findByCountryCode(FALLBACK_COUNTRY))
                .orElseThrow(() -> new IllegalStateException("US pricing not configured"));
        String pricingWarning = regionPrice.isEmpty() ? FALLBACK_WARNING : null;

        CustomerReference customer = paymentGateway.createCustomer(company.getAdminContact(), company.getId());
        PaymentIntentResult paymentIntent = paymentGateway.createPaymentIntent(customer, price);

        session.recordPaymentIntent(paymentIntent.id(), paymentIntent.clientSecret());
        company.initiatePayment();

        sessionRepository.update(session);
        companyRepository.update(company);

        return new InitiatePaymentResult(paymentIntent.clientSecret(), pricingWarning);
    }
}
