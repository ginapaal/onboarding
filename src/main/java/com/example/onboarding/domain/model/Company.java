package com.example.onboarding.domain.model;

// Stub — full aggregate with state machine and invariants to be implemented
public class Company {

    private final CompanyId id;
    private CompanyStatus status;

    public Company(CompanyId id, CompanyStatus status) {
        this.id = id;
        this.status = status;
    }

    public CompanyId getId() {
        return id;
    }

    public CompanyStatus getStatus() {
        return status;
    }
}
