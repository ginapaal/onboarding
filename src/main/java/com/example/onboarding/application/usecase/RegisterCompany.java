package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.SessionId;
import com.example.onboarding.domain.port.inbound.RegistrationResult;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterCompany {

    private final CompanyRepository companyRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public RegistrationResult execute(String companyName, ContactInfo adminContact) {
        CompanyId companyId = CompanyId.generate();
        SessionId sessionId = SessionId.generate();

        companyRepository.save(Company.register(companyId, companyName, adminContact));
        sessionRepository.save(OnboardingSession.create(sessionId, companyId));

        return new RegistrationResult(sessionId, companyId);
    }
}
