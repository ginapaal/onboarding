package com.example.onboarding.domain.model;

import com.example.onboarding.domain.exception.InvalidCompanyStateException;
import com.example.onboarding.domain.exception.MaxRetriesExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

    private Company company;

    @BeforeEach
    void setUp() {
        company = Company.register(
                CompanyId.generate(),
                "Acme Corp",
                new ContactInfo("admin@acme.com", "Jane", "Doe")
        );
    }

    @Test
    void register_createsCompanyWithIncompleteStatus() {
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.INCOMPLETE);
        assertThat(company.getRetryCount()).isZero();
        assertThat(company.getStripeCustomerReference()).isNull();
    }

    @Test
    void initiateActivation_transitionsToPendingActivation() {
        company.initiateActivation();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING_ACTIVATION);
    }

    @Test
    void initiateActivation_fromInvalidStatus_throws() {
        company.initiateActivation();
        assertThatThrownBy(company::initiateActivation)
                .isInstanceOf(InvalidCompanyStateException.class)
                .hasMessageContaining("PENDING_ACTIVATION");
    }

    @Test
    void activate_fromPendingActivation_transitionsToActive() {
        company.initiateActivation();
        company.activate();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void activate_fromActionRequired_transitionsToActive() {
        company.initiateActivation();
        company.actionRequired();
        company.activate();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void activate_fromActivationProcessing_transitionsToActive() {
        company.initiateActivation();
        company.activationProcessing();
        company.activate();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void activationFailed_fromPendingActivation_transitionsToActivationFailed() {
        company.initiateActivation();
        company.activationFailed();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_FAILED);
    }

    @Test
    void activationFailed_fromActivationProcessing_transitionsToActivationFailed() {
        company.initiateActivation();
        company.activationProcessing();
        company.activationFailed();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_FAILED);
    }

    @Test
    void actionRequired_fromPendingActivation_transitionsToActionRequired() {
        company.initiateActivation();
        company.actionRequired();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTION_REQUIRED);
    }

    @Test
    void activationProcessing_fromPendingActivation_transitionsToActivationProcessing() {
        company.initiateActivation();
        company.activationProcessing();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_PROCESSING);
    }

    @Test
    void activationCanceled_fromPendingActivation_transitionsToActivationCanceled() {
        company.initiateActivation();
        company.activationCanceled();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_CANCELED);
    }

    @Test
    void activationCanceled_fromActionRequired_transitionsToActivationCanceled() {
        company.initiateActivation();
        company.actionRequired();
        company.activationCanceled();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVATION_CANCELED);
    }

    @Test
    void retryActivation_incrementsRetryCountAndTransitionsToPendingActivation() {
        company.initiateActivation();
        company.activationFailed();
        company.retryActivation(3);
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING_ACTIVATION);
        assertThat(company.getRetryCount()).isEqualTo(1);
    }

    @Test
    void retryActivation_whenMaxAttemptsExceeded_throws() {
        company.initiateActivation();
        company.activationFailed();
        company.retryActivation(3); // retry 1
        company.activationFailed();
        company.retryActivation(3); // retry 2 — maxAttempts=3 allows 2 retries
        company.activationFailed();
        assertThatThrownBy(() -> company.retryActivation(3)) // retry 3 → throws
                .isInstanceOf(MaxRetriesExceededException.class);
    }

    @Test
    void assignStripeCustomer_setsReference() {
        CustomerReference reference = new CustomerReference("cus_abc123");
        company.assignStripeCustomer(reference);
        assertThat(company.getStripeCustomerReference()).isEqualTo(reference);
    }
}
