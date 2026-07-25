package com.example.onboarding.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OnboardingSession {

    private final OnboardingSessionId id;
    private final CompanyId companyId;
    private StripePaymentIntentId paymentIntentId;

    public static OnboardingSession create(OnboardingSessionId id, CompanyId companyId) {
        return new OnboardingSession(id, companyId, null);
    }

    public void recordPaymentIntent(StripePaymentIntentId paymentIntentId) {
        if (this.paymentIntentId != null) {
            throw new IllegalStateException("Payment intent already recorded for session " + id.value());
        }
        this.paymentIntentId = paymentIntentId;
    }
}
