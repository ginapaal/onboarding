package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
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
@Import({CompanyJdbcRepository.class, FlywayAutoConfiguration.class})
class CompanyJdbcRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CompanyJdbcRepository repository;

    @Test
    void insert_persistsAllFields() {
        Company company = Company.register(
                CompanyId.generate(),
                "Acme Corp",
                new ContactInfo("admin@acme.com", "Jane", "Doe")
        );

        repository.insert(company);

        Optional<Company> found = repository.findById(company.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCompanyName()).isEqualTo("Acme Corp");
        assertThat(found.get().getAdminContact().email()).isEqualTo("admin@acme.com");
        assertThat(found.get().getAdminContact().firstName()).isEqualTo("Jane");
        assertThat(found.get().getAdminContact().lastName()).isEqualTo("Doe");
        assertThat(found.get().getStatus()).isEqualTo(CompanyStatus.INCOMPLETE);
        assertThat(found.get().getRetryCount()).isZero();
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        assertThat(repository.findById(CompanyId.generate())).isEmpty();
    }

    @Test
    void update_persistsStatusChange() {
        Company company = Company.register(
                CompanyId.generate(),
                "Acme Corp",
                new ContactInfo("admin@acme.com", "Jane", "Doe")
        );
        repository.insert(company);

        company.initiatePayment();
        repository.update(company);

        Optional<Company> found = repository.findById(company.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(CompanyStatus.PENDING_ACTIVATION);
    }

    @Test
    void update_persistsRetryCount() {
        Company company = Company.register(
                CompanyId.generate(),
                "Acme Corp",
                new ContactInfo("admin@acme.com", "Jane", "Doe")
        );
        repository.insert(company);

        company.initiatePayment();
        company.paymentFailed();
        company.retryPayment(3);
        repository.update(company);

        Optional<Company> found = repository.findById(company.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(CompanyStatus.PENDING_ACTIVATION);
        assertThat(found.get().getRetryCount()).isEqualTo(1);
    }
}
