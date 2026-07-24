CREATE TABLE onboarding_sessions (
    session_id        UUID         PRIMARY KEY,
    company_id        UUID         NOT NULL REFERENCES companies(company_id),
    payment_intent_id VARCHAR(255),
    client_secret     TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
