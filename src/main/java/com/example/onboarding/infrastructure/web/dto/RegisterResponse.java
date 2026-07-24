package com.example.onboarding.infrastructure.web.dto;

import java.util.UUID;

public record RegisterResponse(UUID sessionId, UUID companyId) {}
