package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.OnboardingSessionId;

public interface GetOnboardingStatusUseCase {

    CompanyStatus execute(OnboardingSessionId sessionId);
}
