package com.example.onboarding.infrastructure.web;

import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.port.inbound.GetOnboardingStatusUseCase;
import com.example.onboarding.domain.port.inbound.InitiatePaymentResult;
import com.example.onboarding.domain.port.inbound.InitiatePaymentUseCase;
import com.example.onboarding.domain.port.inbound.RegisterCompanyUseCase;
import com.example.onboarding.domain.port.inbound.RegistrationResult;
import com.example.onboarding.infrastructure.web.dto.InitiatePaymentRequest;
import com.example.onboarding.infrastructure.web.dto.InitiatePaymentResponse;
import com.example.onboarding.infrastructure.web.dto.OnboardingStatusResponse;
import com.example.onboarding.infrastructure.web.dto.RegisterRequest;
import com.example.onboarding.infrastructure.web.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OnboardingController implements OnboardingPort {

    private final RegisterCompanyUseCase registerCompany;
    private final InitiatePaymentUseCase initiatePayment;
    private final GetOnboardingStatusUseCase getOnboardingStatus;

    @Override
    public ResponseEntity<RegisterResponse> handleRegister(RegisterRequest request) {
        ContactInfo contact = new ContactInfo(request.adminEmail(), request.adminFirstName(), request.adminLastName());
        RegistrationResult result = registerCompany.execute(request.companyName(), contact);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(result.sessionId().value(), result.companyId().value()));
    }

    @Override
    public ResponseEntity<InitiatePaymentResponse> handleInitiatePayment(UUID sessionId, InitiatePaymentRequest request) {
        InitiatePaymentResult result = initiatePayment.execute(new OnboardingSessionId(sessionId), request.ipCountry());
        return ResponseEntity.accepted().body(new InitiatePaymentResponse(result.clientSecret(), result.pricingWarning()));
    }

    @Override
    public ResponseEntity<OnboardingStatusResponse> handleGetStatus(UUID sessionId) {
        CompanyStatus status = getOnboardingStatus.execute(new OnboardingSessionId(sessionId));
        return ResponseEntity.ok(new OnboardingStatusResponse(status.name()));
    }
}
