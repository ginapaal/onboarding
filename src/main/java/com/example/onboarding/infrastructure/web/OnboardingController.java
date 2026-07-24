package com.example.onboarding.infrastructure.web;

import com.example.onboarding.application.usecase.InitiatePayment;
import com.example.onboarding.application.usecase.InitiatePaymentResult;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.SessionId;
import com.example.onboarding.domain.port.inbound.OnboardingPort;
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

    private final InitiatePayment initiatePaymentUseCase;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> handleRegister(@RequestBody @Valid RegisterRequest request) {
        ContactInfo contact = new ContactInfo(request.adminEmail(), request.adminFirstName(), request.adminLastName());
        RegistrationResult result = register(request.companyName(), contact);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(result.sessionId().value(), result.companyId().value()));
    }

    @PostMapping("/{sessionId}/payment")
    public ResponseEntity<InitiatePaymentResponse> handleInitiatePayment(
            @PathVariable UUID sessionId,
            @RequestBody @Valid InitiatePaymentRequest request) {
        InitiatePaymentResult result = initiatePaymentUseCase.execute(new SessionId(sessionId), request.ipCountry());
        return ResponseEntity.accepted().body(new InitiatePaymentResponse(result.clientSecret(), result.pricingWarning()));
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<OnboardingStatusResponse> handleGetStatus(@PathVariable UUID sessionId) {
        CompanyStatus status = getStatus(new SessionId(sessionId));
        return ResponseEntity.ok(new OnboardingStatusResponse(status.name()));
    }

    @Override
    public RegistrationResult register(String companyName, ContactInfo adminContact) {
        return new RegistrationResult(SessionId.generate(), CompanyId.generate());
    }

    @Override
    public void initiatePayment(SessionId sessionId) {
        // superseded by handleInitiatePayment — wired directly to use case
    }

    @Override
    public CompanyStatus getStatus(SessionId sessionId) {
        return CompanyStatus.INCOMPLETE;
    }
}
