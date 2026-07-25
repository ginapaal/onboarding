package com.example.onboarding.domain.model;

import com.example.onboarding.domain.exception.MaxRetriesExceededException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Predicate;

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

    public void initiateActivation() {
        transitionTo(current -> current == CompanyStatus.INCOMPLETE, CompanyStatus.PENDING_ACTIVATION);
    }

    public void activate() {
        transitionTo(
                current -> current == CompanyStatus.PENDING_ACTIVATION
                        || current == CompanyStatus.ACTION_REQUIRED
                        || current == CompanyStatus.ACTIVATION_PROCESSING,
                CompanyStatus.ACTIVE
        );
    }

    public void activationFailed() {
        transitionTo(
                current -> current == CompanyStatus.PENDING_ACTIVATION
                        || current == CompanyStatus.ACTION_REQUIRED
                        || current == CompanyStatus.ACTIVATION_PROCESSING,
                CompanyStatus.ACTIVATION_FAILED
        );
    }

    public void actionRequired() {
        transitionTo(current -> current == CompanyStatus.PENDING_ACTIVATION, CompanyStatus.ACTION_REQUIRED);
    }

    public void activationProcessing() {
        transitionTo(current -> current == CompanyStatus.PENDING_ACTIVATION, CompanyStatus.ACTIVATION_PROCESSING);
    }

    public void activationCanceled() {
        transitionTo(
                current -> current == CompanyStatus.PENDING_ACTIVATION || current == CompanyStatus.ACTION_REQUIRED,
                CompanyStatus.ACTIVATION_CANCELED
        );
    }

    public void retryActivation(int maxAttempts) {
        if (retryCount >= maxAttempts - 1) {
            throw new MaxRetriesExceededException(maxAttempts);
        }
        transitionTo(current -> current == CompanyStatus.ACTIVATION_FAILED, CompanyStatus.PENDING_ACTIVATION);
        retryCount++;
    }

    public void escalateToSupport() {
        this.status = CompanyStatus.REQUIRES_SUPPORT;
    }

    private void transitionTo(Predicate<CompanyStatus> allowedFromStatus, CompanyStatus next) {
        if (!allowedFromStatus.test(this.status)) {
            throw new IllegalStateException("Cannot transition to " + next + " from " + this.status);
        }
        this.status = next;
    }
}
