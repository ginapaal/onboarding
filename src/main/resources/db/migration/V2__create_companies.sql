CREATE TABLE IF NOT EXISTS companies (
    id               BIGSERIAL    PRIMARY KEY,
    company_id       UUID         NOT NULL UNIQUE,
    company_name     VARCHAR(255) NOT NULL,
    admin_email      VARCHAR(255) NOT NULL,
    admin_first_name VARCHAR(100) NOT NULL,
    admin_last_name  VARCHAR(100) NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    retry_count      INT          NOT NULL DEFAULT 0,
    stripe_customer_id VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (company_name, admin_email)
);
