package com.example.onboarding.domain.port;

import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.SessionId;
import com.example.onboarding.domain.model.StripePaymentIntentId;

import java.util.Optional;

public interface SessionRepository {

    void save(OnboardingSession session);

    Optional<OnboardingSession> findById(SessionId id);

    Optional<OnboardingSession> findByPaymentIntentId(StripePaymentIntentId paymentIntentId);
}
