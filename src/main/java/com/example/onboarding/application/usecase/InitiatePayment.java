package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.SessionId;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.PaymentGateway;
import com.example.onboarding.domain.port.outbound.SessionRepository;
import com.example.onboarding.infrastructure.config.PaymentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

@Service
@RequiredArgsConstructor
public class InitiatePayment {

    private final SessionRepository sessionRepository;
    private final CompanyRepository companyRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentConfig paymentConfig;

    @Transactional
    public String execute(SessionId sessionId) {
        OnboardingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        Company company = companyRepository.findById(session.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(session.getCompanyId()));

        CustomerReference customer = paymentGateway.createCustomer(company.getAdminContact(), company.getId());
        PaymentIntentResult paymentIntent = paymentGateway.createPaymentIntent(
                customer,
                new Money(paymentConfig.amountInMinorUnits(), Currency.getInstance(paymentConfig.currency()))
        );

        session.recordPaymentIntent(paymentIntent.id(), paymentIntent.clientSecret());
        company.initiatePayment();

        sessionRepository.save(session);
        companyRepository.save(company);

        return paymentIntent.clientSecret();
    }
}
