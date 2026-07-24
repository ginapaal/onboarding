package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.OnboardingSessionId;

public record RegistrationResult(OnboardingSessionId sessionId, CompanyId companyId) {}
