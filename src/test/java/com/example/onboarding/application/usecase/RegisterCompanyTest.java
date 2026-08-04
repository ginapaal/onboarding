package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.model.ChannelType;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.NotificationOutboxMessage;
import com.example.onboarding.domain.model.NotificationType;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.RegistrationResult;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.NotificationOutboxRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterCompanyTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private NotificationOutboxRepository notificationOutboxRepository;

    @InjectMocks
    private RegisterCompany registerCompany;

    @Test
    void execute_persistsCompanyAndSession() {
        ContactInfo contact = new ContactInfo("admin@acme.com", "Jane", "Doe");

        registerCompany.execute("Acme Corp", contact);

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        ArgumentCaptor<OnboardingSession> sessionCaptor = ArgumentCaptor.forClass(OnboardingSession.class);
        verify(companyRepository).insert(companyCaptor.capture());
        verify(sessionRepository).insert(sessionCaptor.capture());

        Company company = companyCaptor.getValue();
        assertThat(company.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(company.getAdminContact()).isEqualTo(contact);

        OnboardingSession session = sessionCaptor.getValue();
        assertThat(session.getCompanyId()).isEqualTo(company.getId());
    }

    @Test
    void execute_returnsMatchingSessionAndCompanyIds() {
        ContactInfo contact = new ContactInfo("admin@acme.com", "Jane", "Doe");

        RegistrationResult result = registerCompany.execute("Acme Corp", contact);

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        ArgumentCaptor<OnboardingSession> sessionCaptor = ArgumentCaptor.forClass(OnboardingSession.class);
        verify(companyRepository).insert(companyCaptor.capture());
        verify(sessionRepository).insert(sessionCaptor.capture());

        assertThat(result.companyId()).isEqualTo(companyCaptor.getValue().getId());
        assertThat(result.sessionId()).isEqualTo(sessionCaptor.getValue().getId());
    }

    @Test
    void execute_savesRegisteredOutboxEvent() {
        ContactInfo contact = new ContactInfo("admin@acme.com", "Jane", "Doe");

        registerCompany.execute("Acme Corp", contact);

        ArgumentCaptor<NotificationOutboxMessage> captor = ArgumentCaptor.forClass(NotificationOutboxMessage.class);
        verify(notificationOutboxRepository).saveOutboxEvent(captor.capture());

        NotificationOutboxMessage outbox = captor.getValue();
        assertThat(outbox.adminEmail()).isEqualTo("admin@acme.com");
        assertThat(outbox.adminFirstName()).isEqualTo("Jane");
        assertThat(outbox.adminLastName()).isEqualTo("Doe");
        assertThat(outbox.notificationType()).isEqualTo(NotificationType.REGISTERED);
        assertThat(outbox.type()).isEqualTo(ChannelType.EMAIL);
        assertThat(outbox.processed()).isFalse();
    }
}
