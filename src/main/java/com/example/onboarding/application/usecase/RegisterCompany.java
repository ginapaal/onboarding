package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.model.*;
import com.example.onboarding.domain.port.inbound.RegisterCompanyUseCase;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.NotificationOutboxRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterCompany implements RegisterCompanyUseCase {

    private final CompanyRepository companyRepository;
    private final OnboardingSessionRepository sessionRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;

    @Override
    @Transactional
    public RegistrationResult execute(String companyName, ContactInfo adminContact) {
        CompanyId companyId = CompanyId.generate();
        OnboardingSessionId sessionId = OnboardingSessionId.generate();

        companyRepository.insert(Company.register(companyId, companyName, adminContact));
        sessionRepository.insert(OnboardingSession.create(sessionId, companyId));

        notificationOutboxRepository.saveOutboxEvent(new NotificationOutboxMessage(null, companyId, adminContact.email(), adminContact.firstName(), adminContact.lastName(), NotificationType.REGISTERED, ChannelType.EMAIL, false));

        return new RegistrationResult(sessionId, companyId);
    }
}
