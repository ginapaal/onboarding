CREATE TABLE IF NOT EXISTS notification_outbox (
    id                BIGSERIAL    PRIMARY KEY,
    company_id        VARCHAR(255) NOT NULL,
    admin_email       VARCHAR(255) NOT NULL,
    admin_last_name   VARCHAR(255) NOT NULL,
    admin_first_name  VARCHAR(255) NOT NULL,
    channel_type      VARCHAR(10)  NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    processed         BOOLEAN      NOT NULL DEFAULT FALSE
);
