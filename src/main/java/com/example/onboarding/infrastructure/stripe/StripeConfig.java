package com.example.onboarding.infrastructure.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    private final String apiKey;

    public StripeConfig(@Value("${stripe.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
