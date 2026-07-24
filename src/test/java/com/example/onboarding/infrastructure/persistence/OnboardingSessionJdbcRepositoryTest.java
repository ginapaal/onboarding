package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({OnboardingSessionJdbcRepository.class, CompanyJdbcRepository.class, FlywayAutoConfiguration.class})
class OnboardingSessionJdbcRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OnboardingSessionJdbcRepository sessionRepository;

    @Autowired
    private CompanyJdbcRepository companyRepository;

    private CompanyId companyId;

    @BeforeEach
    void insertCompany() {
        companyId = CompanyId.generate();
        companyRepository.insert(Company.register(companyId, "Acme Corp", new ContactInfo("admin@acme.com", "Jane", "Doe")));
    }

    @Test
    void insert_persistsNewSessionWithNullPaymentIntent() {
        OnboardingSession session = OnboardingSession.create(OnboardingSessionId.generate(), companyId);

        sessionRepository.insert(session);

        Optional<OnboardingSession> found = sessionRepository.findById(session.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCompanyId()).isEqualTo(companyId);
        assertThat(found.get().getPaymentIntentId()).isNull();
        assertThat(found.get().getClientSecret()).isNull();
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        assertThat(sessionRepository.findById(OnboardingSessionId.generate())).isEmpty();
    }

    @Test
    void update_persistsPaymentIntent() {
        OnboardingSession session = OnboardingSession.create(OnboardingSessionId.generate(), companyId);
        sessionRepository.insert(session);

        session.recordPaymentIntent(new StripePaymentIntentId("pi_test123"), "pi_test123_secret");
        sessionRepository.update(session);

        Optional<OnboardingSession> found = sessionRepository.findById(session.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentIntentId()).isEqualTo(new StripePaymentIntentId("pi_test123"));
        assertThat(found.get().getClientSecret()).isEqualTo("pi_test123_secret");
    }

    @Test
    void findByPaymentIntentId_returnsSessionAfterPaymentRecorded() {
        OnboardingSession session = OnboardingSession.create(OnboardingSessionId.generate(), companyId);
        sessionRepository.insert(session);
        session.recordPaymentIntent(new StripePaymentIntentId("pi_test123"), "pi_test123_secret");
        sessionRepository.update(session);

        Optional<OnboardingSession> found = sessionRepository.findByPaymentIntentId(new StripePaymentIntentId("pi_test123"));
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(session.getId());
    }
}
