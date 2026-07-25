package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.RetryPaymentResult;

public interface RetryPaymentUseCase {
    RetryPaymentResult execute(OnboardingSessionId currentSessionId, String ipCountry);
}
