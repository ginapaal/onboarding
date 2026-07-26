package com.example.onboarding.domain.port.inbound;

import com.example.onboarding.domain.model.PaymentEvent;

public interface HandlePaymentEventUseCase {
    void execute(PaymentEvent event);
}
