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

### Why hexagonal?

The classic layered architecture (Controller → Service → Repository) tends to blur boundaries
over time: services accumulate Stripe SDK calls, JPA annotations creep into domain objects, and
tests become hard to write without a full Spring context. The core problem is that the domain
*depends on* infrastructure, when it should be the other way around.

Hexagonal architecture (also called Ports & Adapters) inverts this.
The domain sits at the centre and defines *interfaces* (ports) for everything it needs. The
outside world — HTTP, Stripe, the database — provides *implementations* (adapters) that plug
into those ports. Nothing in the domain or application layer imports a framework class or an
SDK type.

This matters concretely for this project in three ways:

1. **Stripe can be swapped without touching domain logic.** `PaymentGateway` is a port. Tests
   use a `MockPaymentGateway`; production uses `StripePaymentGateway`. The state machine and
   use cases are identical in both cases.

2. **The domain is testable in plain Java.** State machine tests, aggregate invariant tests,
   and use case tests need no Spring context, no database, and no Stripe account. They run in
   milliseconds.

3. **The delivery mechanism is a detail.** If we later want a queue consumer or a CLI tool to
   trigger onboarding, we add a driving adapter. The use cases stay unchanged.

### Diagram

```mermaid
flowchart LR
    subgraph drivingSide["DRIVING SIDE"]
        RestClient["REST Client /<br>Browser"]
        OnboardingCtrl["Onboarding<br>Controller<br>(adapter)"]
        WebhookCtrl["Webhook<br>Controller<br>(adapter)"]
        StripeWebhook["Stripe<br>(webhook POST)"]
    end

    subgraph appLayer["Application Layer"]
        RegisterUC["RegisterCompanyUseCase"]
        InitiatePaymentUC["InitiatePaymentUseCase"]
        HandlePaymentUC["HandlePaymentEventUseCase"]
    end

    subgraph domainLayer["Domain Layer"]
        CompanyAggregate["Company<br>(aggregate)"]
        SessionDomain["OnboardingSession"]
        AdminUserDomain["AdminUser"]
        ValueObjects["Value Objects"]
        DomainEvents["Domain Events"]
        StateMachine["State Machine"]
    end

    subgraph drivenPorts["«driven ports» (domain/port defines, infrastructure implements)"]
        CompanyRepoPort["CompanyRepository"]
        SessionRepoPort["SessionRepository"]
        PaymentGateway["PaymentGateway"]
    end

    subgraph appCore["APPLICATION CORE"]
        appLayer
        domainLayer
        drivenPorts
    end

    subgraph drivenSide["DRIVEN SIDE"]
        CompanyRepoImpl["CompanyRepository<br>(JDBC adapter)"]
        SessionRepoImpl["OnboardingSession<br>Repository<br>(JDBC adapter)"]
        StripeGateway["StripePaymentGateway<br>(Stripe SDK adapter)"]
        PostgreSQL[("PostgreSQL")]
        StripeAPI["Stripe API"]
    end

    RestClient -- calls --> OnboardingCtrl
    StripeWebhook -- calls --> WebhookCtrl
    
    OnboardingCtrl -- calls --> RegisterUC
    OnboardingCtrl -- calls --> InitiatePaymentUC
    WebhookCtrl -- calls --> HandlePaymentUC
    
    RegisterUC -. uses .-> CompanyAggregate
    InitiatePaymentUC -. uses .-> CompanyAggregate
    HandlePaymentUC -. uses .-> CompanyAggregate
    HandlePaymentUC -. uses .-> SessionDomain
    
    RegisterUC -- calls --> CompanyRepoPort
    InitiatePaymentUC -- calls --> SessionRepoPort
    InitiatePaymentUC -- calls --> PaymentGateway
    HandlePaymentUC -- calls --> SessionRepoPort
    HandlePaymentUC -- calls --> CompanyRepoPort
    
    CompanyRepoPort -. implemented by .-> CompanyRepoImpl
    SessionRepoPort -. implemented by .-> SessionRepoImpl
    PaymentGateway -. implemented by .-> StripeGateway
    
    CompanyRepoImpl --> PostgreSQL
    SessionRepoImpl --> PostgreSQL
    StripeGateway -- calls --> StripeAPI

    style appLayer fill:#FFF9C4
    style domainLayer fill:#BBDEFB
    style drivenPorts fill:#E1BEE7
    style appCore stroke:#00C853
```


### Ports explained

A port is just a Java interface. The critical rule is **who writes it and who implements it**.

#### Driven ports — defined inside the core, implemented outside

The domain declares what it needs. Infrastructure delivers it.

```
domain/port/CompanyRepository.java          ← interface, lives in the core
infrastructure/persistence/
    CompanyRepositoryJdbc.java              ← implements CompanyRepository, lives in infra
```

```java
// domain/port/CompanyRepository.java
// Pure Java. No Spring, no SQL, no database concept.
public interface CompanyRepository {
    void save(Company company);
    Optional<Company> findById(CompanyId id);
}

// infrastructure/persistence/CompanyRepositoryJdbc.java
// All the database detail lives here, invisible to the domain.
public class CompanyRepositoryJdbc implements CompanyRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void save(Company company) { /* SQL here */ }

    @Override
    public Optional<Company> findById(CompanyId id) { /* SQL here */ }
}
```

Same pattern for `PaymentGateway`:

```java
// domain/port/PaymentGateway.java  ← the domain's requirement, in plain Java
public interface PaymentGateway {
    CustomerReference createCustomer(ContactInfo contact, CompanyId companyId);
    PaymentIntent createPaymentIntent(CustomerReference customer, Money amount);
}

// infrastructure/stripe/StripePaymentGateway.java  ← Stripe SDK confined here
public class StripePaymentGateway implements PaymentGateway { ... }
```

The consequence is the dependency arrow always points **inward**:

```
infrastructure/CompanyRepositoryJdbc   →   domain/port/CompanyRepository
infrastructure/StripePaymentGateway    →   domain/port/PaymentGateway
```

If you deleted every file in `infrastructure/`, the domain and application layers
would still compile. Infrastructure needs the domain; the domain needs nothing.

#### Driving ports — called from outside, implemented inside the core

The controllers (adapters on the driving side) call into use cases. Controllers depend on
use cases; use cases do not know controllers exist.

```java
// application/usecase/RegisterCompany.java
@Service
public class RegisterCompany {
    public RegistrationResult execute(String companyName, ContactInfo adminContact) { ... }
}

// infrastructure/web/OnboardingController.java
// The controller depends on the use case — never the other way around.
@RestController
public class OnboardingController {
    private final RegisterCompany registerCompany;
    ...
}
```

**`OnboardingPort` — the REST API contract**

`OnboardingPort` is an interface in `infrastructure.web` that defines the three HTTP handler
signatures. `OnboardingController` is its only implementation. It lives in the web layer
(not the domain) because its method signatures reference DTO and `ResponseEntity` types —
it is an HTTP contract, not a domain abstraction.

The value is documentation and enforceability: the interface makes the API surface explicit,
and `@Override` on each handler makes it immediately clear which methods satisfy the contract.

**Why inbound port interfaces per use case?**

Consistency with the driven side: every boundary is crossed through an interface. The
controller imports only from `domain.port.inbound` — the same way infrastructure imports
only from `domain.port.outbound`. The `@PostMapping` handlers call the use case interfaces
directly; there is no delegation layer.

The single-entry-point discipline is additionally enforced by an ArchUnit test:

```java
// Every @Service in the application layer may only have one public method, named execute.
methods().that().areDeclaredInClassesThat()
    .resideInAPackage("..application.usecase..")
    .and().areAnnotatedWith(Service.class)
    .and().arePublic()
    .should().haveName("execute");
```

This makes the convention a failing build rather than a code review comment.

### Package layout

```
com.example.onboarding
├── domain
│   ├── model          (Company, AdminUser, OnboardingSession, value objects)
│   ├── event          (CompanyRegistered, PaymentSucceeded, …)
│   └── port           (PaymentGateway, CompanyRepository, SessionRepository)
├── application
│   └── usecase        (RegisterCompanyUseCase, InitiatePaymentUseCase,
│                        HandlePaymentEventUseCase)
└── infrastructure
    ├── persistence    (JDBC adapters, domain↔row mappers)
    ├── stripe         (StripePaymentGateway, StripeWebhookVerifier)
    └── web            (OnboardingController, WebhookController, request/response DTOs)
```

The dependency rule is strictly enforced: `infrastructure` depends on `application` and
`domain`; `application` depends on `domain`; `domain` depends on nothing.

---

## Domain Model

### Aggregates

**`Company`** — aggregate root. Owns the registration and payment lifecycle. Enforces state
transition invariants by throwing a domain exception on illegal transitions (e.g. calling
`paymentSucceeded()` on an already-active company). Tracks a `retryCount` against a
`MAX_RETRIES` constant — when exhausted, `retryPayment()` throws `MaxRetriesExceededException`
and the use case calls `escalateToSupport()`, transitioning the company to `REQUIRES_SUPPORT`.

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
| `PaymentInitiated` | PaymentIntent created, Company → PENDING_ACTIVATION |
| `PaymentSucceeded` | Stripe webhook received, Company → ACTIVE |
| `PaymentFailed` | Stripe webhook received, Company → ACTIVATION_FAILED |
| `ActionRequired` | Stripe webhook received, Company → ACTION_REQUIRED |
| `MaxRetriesExceeded` | `retryPayment()` called at retry limit, Company → REQUIRES_SUPPORT |

**Transport decision:** In-process Spring `ApplicationEventPublisher` for this slice.

*Known limitation:* If the application crashes after the database write but before the event
is published, the event is lost. The correct fix is the **Transactional Outbox** pattern.

**How it works:** instead of publishing the event directly, you write it to an `outbox_events`
table in the *same database transaction* as the domain write. A separate poller then reads
unpublished rows and forwards them to the message bus (e.g. Kafka). Because the event row is
committed atomically with the domain change, a crash can never produce a saved Company without
a corresponding event — the poller will always find and publish it on recovery.

**Benefits:** guaranteed at-least-once delivery; decouples the domain write from the message
bus; pairs naturally with Kafka for cross-service event consumers.

**Tradeoffs:** adds an `outbox_events` table and a poller component; since the poller may
publish an event and crash before marking it as sent, consumers must be idempotent (the same
event may arrive more than once).

This is called out as a named tradeoff — not implemented here for scope reasons.

---

## Company Status State Machine

Transition logic lives as explicit methods on the `Company` aggregate, not in a service.
The aggregate owns the rule "which transitions are valid."

```mermaid
stateDiagram-v2
   [*] --> INCOMPLETE

   INCOMPLETE --> PENDING_ACTIVATION: initiatePayment()

   PENDING_ACTIVATION --> ACTIVE: paymentSucceeded()
   PENDING_ACTIVATION --> ACTION_REQUIRED: actionRequired()
   PENDING_ACTIVATION --> ACTIVATION_FAILED: paymentFailed()

   ACTION_REQUIRED --> ACTIVE: paymentSucceeded()
   ACTION_REQUIRED --> ACTIVATION_FAILED: paymentFailed()

   ACTIVATION_FAILED --> PENDING_ACTIVATION: retryPayment() [retries < MAX_RETRIES]
   ACTIVATION_FAILED --> REQUIRES_SUPPORT: escalateToSupport() [retries >= MAX_RETRIES]

   classDef incomplete fill:#fef2f2,stroke:#f87171
   classDef pending fill:#fff7ed,stroke:#fb923c
   classDef active fill:#f0fdf4,stroke:#4ade80
   classDef actionRequired fill:#ecfeff,stroke:#22d3ee
   classDef failed fill:#fef2f2,stroke:#f87171
   classDef support fill:#f3e8ff,stroke:#a855f7

   class INCOMPLETE incomplete
   class PENDING_ACTIVATION pending
   class ACTIVE active
   class ACTION_REQUIRED actionRequired
   class ACTIVATION_FAILED failed
   class REQUIRES_SUPPORT support
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

### Stripe Response Contracts

What Stripe returns and what we actually keep at the adapter boundary.

#### POST /v1/customers → `CustomerReference`

Stripe returns a full `Customer` object. We only need the ID for subsequent API calls:

```json
{
  "id": "cus_Rk4xQ2abc123",        ← mapped to CustomerReference
  "email": "admin@acme.com",
  "name": "Jane Doe",
  "metadata": { "companyId": "..." },
  "created": 1720000000,
  ... (discarded)
}
```

#### POST /v1/payment_intents → `PaymentIntentResult`

Stripe returns a full `PaymentIntent` object. We keep the ID (for webhook correlation)
and the client secret (for the frontend to confirm payment with Stripe.js):

```json
{
  "id": "pi_3Pxyz_secret_base",     ← mapped to PaymentIntentResult.id
  "client_secret": "pi_3Pxyz_secret_base_abc123",  ← mapped to PaymentIntentResult.clientSecret
  "status": "requires_payment_method",
  "amount": 9900,
  "currency": "usd",
  "customer": "cus_Rk4xQ2abc123",
  ... (discarded)
}
```

The `clientSecret` is returned to the frontend. Stripe.js uses it to confirm the payment
and handle 3DS without any further backend involvement.

#### Webhook events

All events share the same envelope. The `type` field drives routing; `data.object` is the
full resource at the time the event was created.

```json
{
  "id": "evt_1Pabc123",             ← stored in processed_stripe_events for idempotency
  "type": "payment_intent.succeeded | payment_intent.payment_failed | payment_intent.requires_action",
  "data": {
    "object": {
      "id": "pi_3Pxyz",             ← used to look up OnboardingSession
      "status": "succeeded | requires_action | canceled",
      "last_payment_error": {       ← present on payment_intent.payment_failed only
        "code": "card_declined",
        "message": "Your card was declined."
      }
    }
  }
}
```

| Event type | Company transition | What it means |
|---|---|---|
| `payment_intent.succeeded` | → `ACTIVE` | Payment confirmed |
| `payment_intent.payment_failed` | → `ACTIVATION_FAILED` | Card declined or other failure |
| `payment_intent.requires_action` | → `ACTION_REQUIRED` | 3DS or redirect required |

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

## Regional Pricing

Subscription price and currency are resolved server-side from a `regional_prices` table,
seeded by Flyway. The client supplies `ipCountry` (ISO 3166-1 alpha-2) in the payment
initiation request; the server looks up the matching row and uses that `Money` value when
creating the Stripe PaymentIntent.

A `DEFAULT` row acts as a fallback for any country not explicitly listed. When the fallback
is used, a `pricingWarning` is returned in the response so the frontend can surface a message
such as: *"Could not determine pricing for your region. Defaulting to USD."*

### Why client-supplied `ipCountry` rather than server-side derivation?

| Approach | Pros | Cons |
|---|---|---|
| Client sends `ipCountry` | Simple, no extra infrastructure, easy to test | Client can lie — a user could claim a cheaper region |
| Server derives from request IP | Tamper-proof | Requires a geolocation library (MaxMind, ip-api); VPNs still defeat it |

**Decision for this slice:** client-supplied. The implementation is transparent and easy to
verify. The risk of price manipulation is acceptable here because regional pricing differences
reflect currency conversion rather than significant discount tiers. In a production system
where price differences are material, server-side IP resolution would be the right default,
with the geolocation call isolated behind the `PricingRepository` port so it remains
swappable.

### How should pricing data be managed?

Hardcoding `amountInMinorUnits` and `currency` in `application.yml` treats a business
concern (what to charge) as an operational concern (how to configure the service) — the
wrong abstraction. Several better options exist:

| Option | How price changes happen | Who can do it |
|---|---|---|
| Flyway migration (current) | New `.sql` migration file, PR, deployment | Developer |
| Internal admin API | `PUT /admin/pricing/{countryCode}` writes to DB directly | Ops team |
| Third-party CMS (Contentful, Sanity, etc.) | Edit a content entry in the CMS UI | Product owner, PM — no developer needed |
| Stripe Price objects | Update price in the Stripe dashboard | Anyone with Stripe access |

**Decision for this slice:** Flyway-seeded database table, for simplicity. Price changes still
require a migration and a deployment, which is not ideal — but it correctly separates pricing
data from application config and keeps it versioned and auditable alongside the schema.

In a real product, a CMS or an internal admin API would be the right call: product owners
and PMs can update prices or add new regions without raising a ticket or waiting for a
deployment.

**This is straightforward to evolve** because `PricingRepository` is a port — an interface
defined in the domain with no infrastructure knowledge. Swapping the Flyway-backed
`RegionalPriceJdbcRepository` for a `ContentfulPricingAdapter` or `StripePricingAdapter`
requires no changes to `InitiatePayment` or any domain code. The hexagonal boundary makes
this a one-implementation swap.

---

## Failure Scenarios

| Scenario | System behaviour | User experience |
|---|---|---|
| Stripe API down during payment initiation | `PaymentGateway` throws; use case catches and wraps in a domain exception; controller returns HTTP 503 | "Payment service unavailable, please try again" — Company stays `INCOMPLETE`, nothing is persisted |
| Card declined (synchronous) | `payment_intent.payment_failed` webhook → Company → `ACTIVATION_FAILED` | Error message with decline reason; retry button re-uses same registration |
| 3DS required | Stripe.js handles redirect on the frontend; `payment_intent.requires_action` webhook → Company → `ACTION_REQUIRED` | User is redirected by the browser; no action needed from backend during the wait |
| Webhook arrives twice (Stripe retry) | `processed_stripe_events` lookup returns existing row; handler returns HTTP 200 immediately | No effect on Company status |
| Webhook arrives before session is persisted | Race condition during high load; webhook handler finds no `OnboardingSession` | Returns HTTP 404 — Stripe will retry with backoff. Alternatively: short retry loop in handler (not implemented; called out as a known edge case) |
| Invalid registration data | Bean Validation rejects request at controller; HTTP 400 with field-level errors | Inline form errors |
| Retry limit reached | `retryPayment()` throws `MaxRetriesExceededException`; use case calls `escalateToSupport()`; `MaxRetriesExceeded` event published | "Something went wrong with your payment. Please contact support." — prevents Stripe card hammering and protects risk score |

---

## Scalability

Writing to multiple tables per onboarding flow (companies, admin_users, onboarding_sessions,
outbox_events) is not a concern — these are a single database transaction, completing in
milliseconds, with no shared locks between independent company flows. Concurrent registrations
do not block each other.

The real pressure points at high volume, and how to address them:

| Bottleneck | Why it matters | Solution |
|---|---|---|
| Database connections | HikariCP defaults to 10 connections; requests queue under load | Tune pool size; add PgBouncer to multiplex app connections into fewer real DB connections |
| Stripe rate limits | Stripe enforces per-key rate limits; high registration volume triggers 429s | Retry with exponential backoff in `StripePaymentGateway`; queue payment initiations if needed |
| Webhook throughput | Simultaneous payments fire simultaneous webhooks | Service is stateless — scale horizontally; DB-based idempotency guard is correct across all instances with no extra coordination |
| Status polling | Every waiting frontend polling `GET /{sessionId}/status` adds read traffic | Add a read replica for status queries; or replace polling with SSE or WebSocket to push status changes |

**What scales well by design:**
- The service is stateless — horizontal scaling behind a load balancer requires no coordination
- Each company's onboarding flow is fully independent — no cross-company contention
- DB-based idempotency for webhooks works correctly across multiple running instances

---

## Security

### Current posture

All onboarding endpoints are intentionally public in this slice. This is by design, not an
oversight: the user has no prior identity when they arrive — they are registering for the first
time. There is nothing to authenticate against.

| Endpoint | Auth | Reason |
|---|---|---|
| `POST /api/onboarding/register` | None | User does not exist yet |
| `POST /api/onboarding/{sessionId}/payment` | None (sessionId is unguessable) | No identity established yet |
| `GET /api/onboarding/{sessionId}/status` | None (sessionId is unguessable) | Same |
| `POST /webhooks/stripe` | Stripe signature verification | Handled by `Stripe-Signature` header, not user auth |

The `sessionId` is a UUID v4, which provides implicit protection against enumeration — but it
is not a substitute for real authentication.

### When the auth slice is implemented

Once identity is a first-class concern, the following endpoints must be protected:

- `POST /api/onboarding/{sessionId}/payment` — should require a bearer token proving the caller
  is the admin who initiated this registration
- `GET /api/onboarding/{sessionId}/status` — same

The recommended approach is a JWT issued by a dedicated identity service (or Auth0/Okta) once
the admin's email is verified after registration. Spring Security's OAuth2 resource server can
validate it with minimal config. Auth belongs to a separate bounded context and should not be
owned by the onboarding service.

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
| Auth / JWT | Onboarding is a pre-auth flow; no identity exists yet. See Security section. | JWT issued by a dedicated identity service; Spring Security OAuth2 resource server to validate on payment and status endpoints |
| Transactional Outbox | Adds infrastructure complexity (outbox table + poller) not justified without real event consumers | Scheduled poller or Debezium CDC on `outbox_events` publishing to Kafka |
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
