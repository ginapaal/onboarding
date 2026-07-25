package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.StripeWebhookEvent;
import com.example.onboarding.domain.port.inbound.HandlePaymentEventUseCase;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import com.example.onboarding.domain.port.outbound.ProcessedStripeEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandlePaymentEvent implements HandlePaymentEventUseCase {

    private final OnboardingSessionRepository sessionRepository;
    private final CompanyRepository companyRepository;
    private final ProcessedStripeEventRepository processedEventRepository;

    @Override
    @Transactional
    public void execute(StripeWebhookEvent event) {
        if (!processedEventRepository.tryMarkEventProcessed(event.eventId())) {
            return; // duplicate delivery — already processed
        }

        OnboardingSession session = sessionRepository.findByPaymentIntentId(event.paymentIntentId())
                .orElseThrow(() -> new SessionNotFoundException(event.paymentIntentId()));

        Company company = companyRepository.findById(session.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(session.getCompanyId()));

        switch (event.type()) {
            case PAYMENT_SUCCEEDED -> company.activate();
            case PAYMENT_FAILED -> company.activationFailed();
            case PAYMENT_PROCESSING -> company.activationProcessing();
            case PAYMENT_CANCELED -> company.activationCanceled();
            case ACTION_REQUIRED -> company.actionRequired();
        }

        companyRepository.update(company);
    }
}
