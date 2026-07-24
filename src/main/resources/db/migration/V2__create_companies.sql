CREATE TABLE companies (
    company_id       UUID         PRIMARY KEY,
    company_name     VARCHAR(255) NOT NULL,
    admin_email      VARCHAR(255) NOT NULL,
    admin_first_name VARCHAR(100) NOT NULL,
    admin_last_name  VARCHAR(100) NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    retry_count      INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
