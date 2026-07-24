package com.example.onboarding.application.usecase;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOnboardingStatusTest {

    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private GetOnboardingStatus getOnboardingStatus;

    private OnboardingSessionId sessionId;
    private CompanyId companyId;
    private OnboardingSession session;
    private Company company;

    @BeforeEach
    void setUp() {
        sessionId = OnboardingSessionId.generate();
        companyId = CompanyId.generate();
        session = OnboardingSession.create(sessionId, companyId);
        company = Company.register(companyId, "Acme Corp", new ContactInfo("admin@acme.com", "Jane", "Doe"));
    }

    @Test
    void execute_returnsCompanyStatus() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        CompanyStatus status = getOnboardingStatus.execute(sessionId);

        assertThat(status).isEqualTo(CompanyStatus.INCOMPLETE);
    }

    @Test
    void execute_withUnknownSession_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOnboardingStatus.execute(sessionId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void execute_withUnknownCompany_throws() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOnboardingStatus.execute(sessionId))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
