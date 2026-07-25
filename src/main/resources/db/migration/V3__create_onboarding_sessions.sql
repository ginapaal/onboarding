CREATE TABLE onboarding_sessions (
    session_id        UUID         PRIMARY KEY,
    company_id        BIGINT       NOT NULL REFERENCES companies(id),
    payment_intent_id VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
