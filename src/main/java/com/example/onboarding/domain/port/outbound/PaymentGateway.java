package com.example.onboarding.domain.port.outbound;

import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.PaymentIntentResult;

public interface PaymentGateway {

    CustomerReference createCustomer(ContactInfo contact, CompanyId companyId);

    PaymentIntentResult createPaymentIntent(CustomerReference customer, Money amount);
}
