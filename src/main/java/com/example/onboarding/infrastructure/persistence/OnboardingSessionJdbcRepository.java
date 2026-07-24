package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OnboardingSessionJdbcRepository implements OnboardingSessionRepository {

    @Override
    public void insert(OnboardingSession session) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void update(OnboardingSession session) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Optional<OnboardingSession> findById(OnboardingSessionId id) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Optional<OnboardingSession> findByPaymentIntentId(StripePaymentIntentId paymentIntentId) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
