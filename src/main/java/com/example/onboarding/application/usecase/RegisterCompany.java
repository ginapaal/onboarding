package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.RegistrationResult;
import com.example.onboarding.domain.port.inbound.RegisterCompanyUseCase;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterCompany implements RegisterCompanyUseCase {

    private final CompanyRepository companyRepository;
    private final OnboardingSessionRepository sessionRepository;

    @Override
    @Transactional
    public RegistrationResult execute(String companyName, ContactInfo adminContact) {
        CompanyId companyId = CompanyId.generate();
        OnboardingSessionId sessionId = OnboardingSessionId.generate();

        companyRepository.insert(Company.register(companyId, companyName, adminContact));
        sessionRepository.insert(OnboardingSession.create(sessionId, companyId));

        return new RegistrationResult(sessionId, companyId);
    }
}
