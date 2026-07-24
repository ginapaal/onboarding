package com.example.onboarding.domain.model;

// Stub — full aggregate to be implemented
public class OnboardingSession {

    private final SessionId id;
    private final CompanyId companyId;
    private StripePaymentIntentId paymentIntentId;

    public OnboardingSession(SessionId id, CompanyId companyId) {
        this.id = id;
        this.companyId = companyId;
    }

    public SessionId getId() {
        return id;
    }

    public CompanyId getCompanyId() {
        return companyId;
    }

    public StripePaymentIntentId getPaymentIntentId() {
        return paymentIntentId;
    }
}
