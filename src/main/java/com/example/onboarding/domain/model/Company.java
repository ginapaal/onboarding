package com.example.onboarding.domain.model;

import com.example.onboarding.domain.exception.MaxRetriesExceededException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

@Getter
@AllArgsConstructor
public class Company {

    private final CompanyId id;
    private final String companyName;
    private final ContactInfo adminContact;
    private CompanyStatus status;
    private int retryCount;
    private CustomerReference stripeCustomerReference;

    public static Company register(CompanyId id, String companyName, ContactInfo adminContact) {
        return new Company(id, companyName, adminContact, CompanyStatus.INCOMPLETE, 0, null);
    }

    public void assignStripeCustomer(CustomerReference customerReference) {
        this.stripeCustomerReference = customerReference;
    }

    public void initiatePayment() {
        requireStatus(CompanyStatus.INCOMPLETE);
        this.status = CompanyStatus.PENDING_ACTIVATION;
    }

    public void paymentSucceeded() {
        requireStatus(CompanyStatus.PENDING_ACTIVATION, CompanyStatus.ACTION_REQUIRED);
        this.status = CompanyStatus.ACTIVE;
    }

    public void paymentFailed() {
        requireStatus(CompanyStatus.PENDING_ACTIVATION, CompanyStatus.ACTION_REQUIRED);
        this.status = CompanyStatus.ACTIVATION_FAILED;
    }

    public void actionRequired() {
        requireStatus(CompanyStatus.PENDING_ACTIVATION);
        this.status = CompanyStatus.ACTION_REQUIRED;
    }

    public void retryPayment(int maxRetries) {
        requireStatus(CompanyStatus.ACTIVATION_FAILED);
        if (retryCount >= maxRetries) {
            throw new MaxRetriesExceededException(id, retryCount);
        }
        retryCount++;
        this.status = CompanyStatus.PENDING_ACTIVATION;
    }

    public void escalateToSupport() {
        this.status = CompanyStatus.REQUIRES_SUPPORT;
    }

    private void requireStatus(CompanyStatus... allowed) {
        if (!Set.of(allowed).contains(this.status)) {
            throw new IllegalStateException("Expected one of " + Arrays.toString(allowed) + " but was " + this.status);
        }
    }
}
