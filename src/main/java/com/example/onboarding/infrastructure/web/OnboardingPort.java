package com.example.onboarding.infrastructure.web;

import com.example.onboarding.infrastructure.web.dto.InitiatePaymentRequest;
import com.example.onboarding.infrastructure.web.dto.InitiatePaymentResponse;
import com.example.onboarding.infrastructure.web.dto.OnboardingStatusResponse;
import com.example.onboarding.infrastructure.web.dto.RegisterRequest;
import com.example.onboarding.infrastructure.web.dto.RegisterResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface OnboardingPort {

    ResponseEntity<RegisterResponse> handleRegister(RegisterRequest request);

    ResponseEntity<InitiatePaymentResponse> handleInitiatePayment(UUID sessionId, InitiatePaymentRequest request);

    ResponseEntity<OnboardingStatusResponse> handleGetStatus(UUID sessionId);
}
