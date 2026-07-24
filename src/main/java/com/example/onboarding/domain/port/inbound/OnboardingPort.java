package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.SessionId;

public interface OnboardingPort {

    RegistrationResult register(String companyName, ContactInfo adminContact);

    void initiatePayment(SessionId sessionId);

    CompanyStatus getStatus(SessionId sessionId);
}
