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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController implements OnboardingPort {

    private final RegisterCompanyUseCase registerCompany;
    private final InitiatePaymentUseCase initiatePayment;
    private final GetOnboardingStatusUseCase getOnboardingStatus;

    @Override
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> handleRegister(@RequestBody @Valid RegisterRequest request) {
        ContactInfo contact = new ContactInfo(request.adminEmail(), request.adminFirstName(), request.adminLastName());
        RegistrationResult result = registerCompany.execute(request.companyName(), contact);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(result.sessionId().value(), result.companyId().value()));
    }

    @Override
    @PostMapping("/{sessionId}/payment")
    public ResponseEntity<InitiatePaymentResponse> handleInitiatePayment(
            @PathVariable UUID sessionId,
            @RequestBody @Valid InitiatePaymentRequest request) {
        InitiatePaymentResult result = initiatePayment.execute(new OnboardingSessionId(sessionId), request.ipCountry());
        return ResponseEntity.accepted().body(new InitiatePaymentResponse(result.clientSecret(), result.pricingWarning()));
    }

    @Override
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<OnboardingStatusResponse> handleGetStatus(@PathVariable UUID sessionId) {
        CompanyStatus status = getOnboardingStatus.execute(new OnboardingSessionId(sessionId));
        return ResponseEntity.ok(new OnboardingStatusResponse(status.name()));
    }
}
