package com.example.onboarding.infrastructure.web;

import com.example.onboarding.infrastructure.web.dto.InitiatePaymentRequest;
import com.example.onboarding.infrastructure.web.dto.InitiatePaymentResponse;
import com.example.onboarding.infrastructure.web.dto.OnboardingStatusResponse;
import com.example.onboarding.infrastructure.web.dto.RegisterRequest;
import com.example.onboarding.infrastructure.web.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

public interface OnboardingPort {

    @PostMapping("/api/onboarding/register")
    ResponseEntity<RegisterResponse> handleRegister(@RequestBody @Valid RegisterRequest request);

    @PostMapping("/api/onboarding/{sessionId}/payment")
    ResponseEntity<InitiatePaymentResponse> handleInitiatePayment(
            @PathVariable UUID sessionId,
            @RequestBody @Valid InitiatePaymentRequest request);

    @GetMapping("/api/onboarding/{sessionId}/status")
    ResponseEntity<OnboardingStatusResponse> handleGetStatus(@PathVariable UUID sessionId);
}
