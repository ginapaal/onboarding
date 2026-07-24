package com.example.onboarding.infrastructure.stripe;

import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.port.PaymentGateway;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayStub implements PaymentGateway {

    @Override
    public CustomerReference createCustomer(ContactInfo contact, CompanyId companyId) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public PaymentIntentResult createPaymentIntent(CustomerReference customer, Money amount) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
