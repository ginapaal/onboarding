package com.example.onboarding.domain.exception;

import com.example.onboarding.domain.model.CompanyId;

public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(CompanyId id) {
        super("Company not found: " + id.value());
    }
}
