# Design Document

## Problem Statement

> As a company admin, I need to sign up, enter my business and billing details, and complete
> payment so that my account is activated immediately on success, held in a pending state during
> async confirmation (3DS), and can recover safely from payment failures without losing my
> registration data.

---

## Bounded Context

The entire slice lives in the **Onboarding** bounded context. Its single responsibility is to
take a company from "unknown" to "active, paying customer." Payment concepts are wrapped behind
a domain port; the domain model never imports Stripe types directly.

---

## Architecture: Hexagonal (Ports & Adapters)

```
┌──────────────────────────────────────────────────────────┐
│                        domain/                            │
│   model/        event/        port/ (interfaces only)    │
│   (aggregates,  (domain       (PaymentGateway,            │
│    value objs)   events)       CompanyRepository, …)      │
└───────────────────────────┬──────────────────────────────┘
                            │ depends on
          ┌─────────────────┴─────────────────┐
          │          application/              │
          │  usecase/ (one class per use case) │
          └─────────────────┬─────────────────┘
                            │ depends on
          ┌─────────────────┴──────────────────────────────┐
          │              infrastructure/                    │
          │  persistence/   stripe/   web/                  │
          │  (JPA adapters) (Stripe   (controllers,         │
          │                  adapter)  DTOs)                │
          └────────────────────────────────────────────────┘
```

**Why hexagonal:** The domain model and use cases carry zero framework annotations. This makes
them straightforward to unit test and decouples business rules from delivery mechanism choices.

---

## Domain Model

### Aggregates

**`Company`** — aggregate root. Owns the registration and payment lifecycle. Enforces state
transition invariants by throwing a domain exception on illegal transitions (e.g. calling
`paymentSucceeded()` on an already-active company).

**`OnboardingSession`** — separate aggregate, referenced by `CompanyId`. Tracks a single
payment attempt: the Stripe PaymentIntent ID, the client secret returned to the frontend,
and timestamps. Keeping it separate means:
- Payment retry creates a new `OnboardingSession` without mutating `Company` history.
- The payment lifecycle is independently evolvable (future: multiple attempts, audit trail).

**`AdminUser`** — entity inside the `Company` aggregate for this slice. Always accessed through
Company; has no independent lifecycle at this stage.

### Value Objects

All primitive values are wrapped in typed value objects — no raw `String` or `UUID` in method
signatures.

| Value Object | Wraps | Why |
|---|---|---|
| `CompanyId` | `UUID` | Prevents passing wrong ID type across use case boundaries |
| `AdminUserId` | `UUID` | Same |
| `SessionId` | `UUID` | Same |
| `StripeCustomerId` | `String` | Anti-corruption; Stripe internals stay in infrastructure |
| `StripePaymentIntentId` | `String` | Same |
| `Money` | `long` (cents) + `Currency` | Prevents float arithmetic errors; makes currency explicit |
| `BusinessDetails` | name, domain, size | Groups related fields; validated as a unit |
| `ContactInfo` | email, firstName, lastName | Same |

### Domain Events

Published by the aggregate root after a successful state transition.

| Event | Trigger |
|---|---|
| `CompanyRegistered` | `Company` created |
| `PaymentInitiated` | PaymentIntent created, Company → PENDING_PAYMENT |
| `PaymentSucceeded` | Stripe webhook received, Company → ACTIVE |
| `PaymentFailed` | Stripe webhook received, Company → PAYMENT_FAILED |
| `ActionRequired` | Stripe webhook received, Company → AWAITING_ACTION |

**Transport decision:** In-process Spring `ApplicationEventPublisher` for this slice.

*Known limitation:* If the application crashes after the database write but before the event
is published, the event is lost. The correct fix is the **Transactional Outbox** pattern
(write events to an `outbox_events` table in the same transaction, publish via a separate
poller). This is called out as a named tradeoff — not implemented here for scope reasons.

---

## Company Status State Machine

Transition logic lives as explicit methods on the `Company` aggregate, not in a service.
The aggregate owns the rule "which transitions are valid."

```
                  ┌─────────────┐
                  │  INCOMPLETE │  (initial state on registration)
                  └──────┬──────┘
                         │ initiatePayment()
                  ┌──────▼──────┐
                  │   PENDING   │
                  │   PAYMENT   │
                  └──┬──────┬───┘
                     │      │
     paymentFailed() │      │ paymentSucceeded()
                     │      │
          ┌──────────▼─┐  ┌─▼──────┐
          │  PAYMENT   │  │ ACTIVE │
          │   FAILED   │  └────────┘
          └──────┬─────┘
                 │ retryPayment()
                 └──────► PENDING_PAYMENT (new OnboardingSession)

  PENDING_PAYMENT ──actionRequired()──► AWAITING_ACTION
  AWAITING_ACTION ──paymentSucceeded() / paymentFailed()──► ACTIVE / PAYMENT_FAILED
```

---

## Integration: Stripe

### The Port (domain layer)

```java
public interface PaymentGateway {
    CustomerReference createCustomer(ContactInfo contact, CompanyId companyId);
    PaymentIntent createPaymentIntent(CustomerReference customer, Money amount);
}
```

`PaymentIntent` here is the **domain type** — it carries only what the domain needs
(`paymentIntentId`, `clientSecret`, `status`). The Stripe SDK object never crosses into
the domain or application layers.

### The Adapter (infrastructure/stripe)

`StripePaymentGateway` implements `PaymentGateway`. All Stripe SDK imports are confined here.
Stripe API errors are caught and translated into domain exceptions.

### Webhook Flow

```
Stripe ──POST──► /webhooks/stripe
                     │
               verify signature
               (Webhook.constructEvent)
                     │
               check processed_stripe_events
               (idempotency guard)
                     │
               HandlePaymentEventUseCase
                     │
               load OnboardingSession by paymentIntentId
                     │
               load Company by companyId
                     │
               transition Company status
                     │
               save Company + mark event processed
               (same transaction)
```

Signature verification uses `Stripe.constructEvent(rawBody, sigHeader, webhookSecret)`.
**Important:** Spring must be configured to read the raw request body as bytes before
deserialization, otherwise signature verification fails.

---

## Failure Scenarios

| Scenario | System behaviour | User experience |
|---|---|---|
| Stripe API down during payment initiation | `PaymentGateway` throws; use case catches and wraps in a domain exception; controller returns HTTP 503 | "Payment service unavailable, please try again" — Company stays `INCOMPLETE`, nothing is persisted |
| Card declined (synchronous) | `payment_intent.payment_failed` webhook → Company → `PAYMENT_FAILED` | Error message with decline reason; retry button re-uses same registration |
| 3DS required | Stripe.js handles redirect on the frontend; `payment_intent.requires_action` webhook → Company → `AWAITING_ACTION` | User is redirected by the browser; no action needed from backend during the wait |
| Webhook arrives twice (Stripe retry) | `processed_stripe_events` lookup returns existing row; handler returns HTTP 200 immediately | No effect on Company status |
| Webhook arrives before session is persisted | Race condition during high load; webhook handler finds no `OnboardingSession` | Returns HTTP 404 — Stripe will retry with backoff. Alternatively: short retry loop in handler (not implemented; called out as a known edge case) |
| Invalid registration data | Bean Validation rejects request at controller; HTTP 400 with field-level errors | Inline form errors |

---

## Scope Decisions

### Built

- Domain model: `Company`, `AdminUser`, `OnboardingSession`, value objects, state machine
- Use cases: `RegisterCompanyUseCase`, `InitiatePaymentUseCase`, `HandlePaymentEventUseCase`
- Stripe adapter: customer creation, PaymentIntent creation, webhook event construction
- Webhook handler: signature verification, idempotency via `processed_stripe_events` table
- REST API: register, initiate payment, poll status, webhook receiver
- Flyway schema migrations
- Unit tests: state machine transitions, webhook idempotency

### Described but not built

| Omission | Reason | What production would need |
|---|---|---|
| Frontend / Stripe.js | Out of scope for backend slice | React or plain JS with `@stripe/stripe-js` confirming the PaymentIntent |
| Auth / JWT | Not the focus of this slice | Spring Security + JWT or Auth0 |
| Transactional Outbox | Added complexity for marginal gain in demo context | Debezium or a scheduled poller on `outbox_events` |
| Email notifications | Infrastructure concern, not domain | Spring Mail or SendGrid on `CompanyActivated` event |
| Rate limiting | Ops concern | Bucket4j or API gateway |
| Payment retry UX | Depends on product decisions not yet made | New `OnboardingSession` per attempt; frontend polls status |

---

## Key Tradeoffs

| Decision | Chosen | Alternative | Why |
|---|---|---|---|
| `OnboardingSession` as separate aggregate | Yes | Entity inside Company | Independent evolvability; retry creates new session without mutating Company history |
| Hexagonal architecture | Yes | Classic layered | Domain has zero framework deps; easier to test; aligns with DDD |
| Typed value objects | Yes | Raw `String`/`UUID` | Compile-time safety across use case boundaries |
| In-process domain events | Yes | Outbox pattern | Simpler for this scope; limitation is named explicitly |
| Idempotency via DB table | Yes | In-memory set | Survives restarts; correct under concurrent webhook delivery |
| Stripe real test mode | Yes | Mock | Demonstrates real contract understanding; webhook signature verification is non-trivial |
