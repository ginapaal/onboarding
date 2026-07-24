# Onboarding Service

Self-serve business onboarding with Stripe payment integration.

> As a company admin, I need to sign up, enter my business and billing details, and complete
> payment so that my account is activated immediately on success, held in a pending state during
> async confirmation (3DS), and can recover safely from payment failures without losing my
> registration data.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- A [Stripe](https://stripe.com) account (test mode keys)
- [Stripe CLI](https://docs.stripe.com/stripe-cli) (for local webhook forwarding)

## Getting Started

### 1. Start the database

```bash
docker-compose up -d
```

### 2. Set environment variables

```bash
export STRIPE_API_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
```

You can find your test API key in the [Stripe Dashboard](https://dashboard.stripe.com/test/apikeys).
The webhook secret is generated when you run the Stripe CLI in step 4.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

### 4. Forward Stripe webhooks (local development)

```bash
stripe listen --forward-to localhost:8080/webhooks/stripe
```

Copy the `whsec_...` secret printed by the CLI and set it as `STRIPE_WEBHOOK_SECRET`.

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/onboarding/register` | Register company + admin user |
| `POST` | `/api/onboarding/{sessionId}/payment` | Create Stripe PaymentIntent, return clientSecret |
| `GET`  | `/api/onboarding/{sessionId}/status` | Poll current onboarding status |
| `POST` | `/webhooks/stripe` | Stripe webhook receiver (signature-verified) |

## Stripe Test Cards

| Card number | Scenario |
|---|---|
| `4242 4242 4242 4242` | Payment succeeds immediately |
| `4000 0025 0000 3155` | 3DS authentication required |
| `4000 0000 0000 9995` | Insufficient funds (decline) |

Use any future expiry date, any 3-digit CVC, and any postal code.

## Project Structure

```
src/main/java/com/example/onboarding/
├── domain/               # Pure domain model — no framework dependencies
│   ├── model/            # Aggregates, entities, value objects
│   ├── event/            # Domain events
│   └── port/             # Repository and gateway interfaces
├── application/
│   └── usecase/          # One class per use case
└── infrastructure/
    ├── persistence/      # JPA entities, repositories, mappers
    ├── stripe/           # Stripe SDK adapter
    └── web/              # Controllers, request/response DTOs
```

See [DESIGN.md](./DESIGN.md) for architecture decisions and tradeoffs.

## Running Tests

```bash
./mvnw test
```

Tests use an H2 in-memory database and a mocked Stripe gateway — no real Stripe calls are made.
