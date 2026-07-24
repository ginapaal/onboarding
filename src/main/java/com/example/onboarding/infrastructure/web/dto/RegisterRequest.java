package com.example.onboarding.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String companyName,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminFirstName,
        @NotBlank String adminLastName
) {}
