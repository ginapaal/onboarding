package com.example.onboarding.infrastructure.stripe;

import com.example.onboarding.domain.exception.PaymentGatewayException;
import com.example.onboarding.domain.exception.PaymentGatewayUnavailableException;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.model.CustomerReference;
import com.example.onboarding.domain.model.Money;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentResult;
import com.example.onboarding.domain.model.StripePaymentIntentId;
import com.example.onboarding.domain.port.outbound.PaymentGateway;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public CustomerReference createCustomer(ContactInfo contact, CompanyId companyId) {
        try {
            Customer customer = Customer.create(
                    getCustomerParams(contact, companyId),
                    getIdempotencyOptions("cus-" + companyId.value())
            );
            return new CustomerReference(customer.getId());
        } catch (StripeException e) {
            throw translateStripeException(e, "Failed to create Stripe customer for company " + companyId.value());
        }
    }

    @Override
    public PaymentIntentResult createPaymentIntent(CustomerReference customer, Money amount, OnboardingSessionId sessionId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.create(
                    getPaymentIntentParams(customer, amount),
                    getIdempotencyOptions("pi-" + sessionId.value())
            );
            return new PaymentIntentResult(
                    new StripePaymentIntentId(paymentIntent.getId()),
                    paymentIntent.getClientSecret()
            );
        } catch (StripeException e) {
            throw translateStripeException(e, "Failed to create Stripe PaymentIntent for session " + sessionId.value());
        }
    }

    private CustomerCreateParams getCustomerParams(ContactInfo contact, CompanyId companyId) {
        return CustomerCreateParams.builder()
                .setEmail(contact.email())
                .setName(contact.firstName() + " " + contact.lastName())
                .putMetadata("companyId", companyId.value().toString())
                .build();
    }

    private PaymentIntentCreateParams getPaymentIntentParams(CustomerReference customer, Money amount) {
        return PaymentIntentCreateParams.builder()
                .setAmount(amount.amountInMinorUnits())
                .setCurrency(amount.currency().getCurrencyCode().toLowerCase())
                .setCustomer(customer.value())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build()
                )
                .build();
    }

    private RequestOptions getIdempotencyOptions(String key) {
        return RequestOptions.builder()
                .setIdempotencyKey(key)
                .build();
    }

    private RuntimeException translateStripeException(StripeException exception, String context) {
        if (exception instanceof ApiConnectionException || exception instanceof RateLimitException) {
            log.warn("Transient Stripe error — {}: {}", context, exception.getMessage());
            return new PaymentGatewayUnavailableException(context, exception);
        }
        log.error("Permanent Stripe error — {}: {}", context, exception.getMessage(), exception);
        return new PaymentGatewayException(context, exception);
    }
}
