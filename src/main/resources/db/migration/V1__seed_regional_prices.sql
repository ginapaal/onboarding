CREATE TABLE regional_prices (
    country_code           VARCHAR(10) PRIMARY KEY,
    amount_in_minor_units  BIGINT      NOT NULL,
    currency               CHAR(3)     NOT NULL
);

INSERT INTO regional_prices (country_code, amount_in_minor_units, currency) VALUES
('US',      9900,  'USD'),
('GB',      7900,  'GBP'),
('DE',      8900,  'EUR'),
('FR',      8900,  'EUR'),
('CA',      12900, 'CAD'),
('AU',      14900, 'AUD');
