package com.example.onboarding.domain.port;

import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.SessionId;

public record RegistrationResult(SessionId sessionId, CompanyId companyId) {}
