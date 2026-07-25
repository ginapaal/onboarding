package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.InitiatePaymentResult;
import com.example.onboarding.domain.model.OnboardingSessionId;

public interface InitiatePaymentUseCase {

    InitiatePaymentResult execute(OnboardingSessionId sessionId, String ipCountry);
}
