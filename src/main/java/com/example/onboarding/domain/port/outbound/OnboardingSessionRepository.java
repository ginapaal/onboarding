package com.example.onboarding.domain.port.outbound;

import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentId;

import java.util.Optional;

public interface OnboardingSessionRepository {

    void insert(OnboardingSession session);

    void update(OnboardingSession session);

    Optional<OnboardingSession> findById(OnboardingSessionId id);

    Optional<OnboardingSession> findByPaymentIntentId(PaymentIntentId paymentIntentId);
}
