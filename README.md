# Onboarding Service

Self-serve business onboarding with Stripe payment integration.

> As a company admin, I need to register my company and complete a subscription payment,
> so that my account activates immediately on success, remains accessible while awaiting
> async payment confirmation, and lets me retry a declined payment without having to start over.

> **Note:** The service is deployed to [Railway](https://railway.app). For testing instructions, see [TESTING.md](./TESTING.md).

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- A [Stripe](https://stripe.com) account (test mode keys)
- [Stripe CLI](https://docs.stripe.com/stripe-cli) (for local webhook forwarding)

### Java version management

This project requires **Java 21**. A `.java-version` file is committed at the repo root.
If you have multiple JDKs installed, use [jenv](https://www.jenv.be) to ensure Maven picks
up the correct one:

```bash
brew install jenv
jenv add $(/usr/libexec/java_home -v 21)
jenv local 21
```

Without this, Maven may run on a newer JDK where Lombok annotation processing fails silently,
causing missing constructors and getters at compile time.

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
| `POST` | `/api/v1/onboarding/companies/register` | Register company + admin user |
| `POST` | `/api/v1/onboarding/{sessionId}/payments` | Create Stripe PaymentIntent, return clientSecret |
| `GET`  | `/api/v1/onboarding/{sessionId}/status` | Poll current onboarding status |
| `POST` | `/api/v1/onboarding/{sessionId}/payments/retry` | Retry payment after a decline (new session + new PaymentIntent) |
| `POST` | `/webhooks/stripe` | Stripe webhook receiver (signature-verified) |

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
    ├── persistence/      # JDBC adapters, domain↔row mappers
    ├── stripe/           # Stripe SDK adapter
    └── web/              # Controllers, request/response DTOs
```

See [DESIGN.md](./DESIGN.md) for architecture decisions and tradeoffs.

## Deployment

The service is hosted on [Railway](https://railway.app). Railway provisions a managed PostgreSQL
database and injects connection variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`,
`PGPASSWORD`) automatically — no Docker needed in production.

### Environment variables (set in Railway dashboard → service → Variables)

| Variable | Value |
|---|---|
| `STRIPE_API_KEY` | Your Stripe test or live secret key |
| `STRIPE_WEBHOOK_SECRET` | Webhook signing secret from the Stripe dashboard |

### Stripe webhooks in production

In the [Stripe Dashboard](https://dashboard.stripe.com/test/webhooks), add an endpoint pointing
to your Railway service URL:

```
https://<your-railway-url>/webhooks/stripe
```

Subscribe to the events: `payment_intent.succeeded`, `payment_intent.payment_failed`,
`payment_intent.processing`, `payment_intent.canceled`, `payment_intent.requires_action`.

Copy the signing secret Railway surfaces after saving and set it as `STRIPE_WEBHOOK_SECRET`.
The Stripe CLI (`stripe listen`) is only needed for local development.

### Deploys

Railway redeploys automatically on every push to the connected branch — no manual restarts needed.

---

## Running Tests

```bash
./mvnw test
```

Persistence tests run against a real PostgreSQL instance via Testcontainers (Docker required). Use case tests mock the gateway interfaces — no real Stripe calls are made.
