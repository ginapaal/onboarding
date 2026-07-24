package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSessionId;

public interface OnboardingPort {

    RegistrationResult register(String companyName, ContactInfo adminContact);

    void initiatePayment(OnboardingSessionId sessionId);

    CompanyStatus getStatus(OnboardingSessionId sessionId);
}
