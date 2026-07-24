package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.ContactInfo;

public interface RegisterCompanyUseCase {

    RegistrationResult execute(String companyName, ContactInfo adminContact);
}
