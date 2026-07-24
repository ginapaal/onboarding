package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.SessionId;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.port.outbound.SessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SessionJdbcRepository implements SessionRepository {

    @Override
    public void save(OnboardingSession session) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Optional<OnboardingSession> findById(SessionId id) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Optional<OnboardingSession> findByPaymentIntentId(StripePaymentIntentId paymentIntentId) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
