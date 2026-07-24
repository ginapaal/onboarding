package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.port.inbound.GetOnboardingStatusUseCase;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOnboardingStatus implements GetOnboardingStatusUseCase {

    private final OnboardingSessionRepository sessionRepository;
    private final CompanyRepository companyRepository;

    @Override
    public CompanyStatus execute(OnboardingSessionId sessionId) {
        OnboardingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        return companyRepository.findById(session.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(session.getCompanyId()))
                .getStatus();
    }
}
