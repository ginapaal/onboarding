package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.RegistrationResult;

public interface RegisterCompanyUseCase {

    RegistrationResult execute(String companyName, ContactInfo adminContact);
}
