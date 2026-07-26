# Stripe Integration

## The Port (domain layer)

```java
public interface PaymentGateway {
    CustomerReference createCustomer(ContactInfo contact, CompanyId companyId);
    PaymentIntentResult createPaymentIntent(CustomerReference customer, Money amount, OnboardingSessionId sessionId);
}
```

`PaymentIntentResult` is the domain type — it carries only what the domain needs
(`paymentIntentId`, `clientSecret`). The Stripe SDK object never crosses into the domain
or application layers.

---

## The Adapter (infrastructure/stripe)

`StripePaymentGateway` implements `PaymentGateway`. All Stripe SDK imports are confined here.
`StripeConfig` sets `Stripe.apiKey` once at startup via `@PostConstruct`.

Stripe API errors are caught and re-thrown as `PaymentGatewayException` — a domain exception
that carries the message and original cause for logging. The Stripe error code never leaks to
the client.

---

## Idempotency Keys

Both Stripe calls carry idempotency keys so that a retry after a timeout or crash does not
create duplicate Stripe objects:

| Call | Idempotency key | Rationale |
|---|---|---|
| `createCustomer` | `"cus-{companyId}"` | One company → one Stripe customer, always |
| `createPaymentIntent` | `"pi-{sessionId}"` | One `OnboardingSession` represents one payment attempt |

If the service crashes after a Stripe call but before committing to the database, the
retry gets the same Stripe object back rather than creating a second one.

### CustomerReference caching

`Company` stores its Stripe customer ID as `stripeCustomerReference` (persisted in
`companies.stripe_customer_id`). `InitiatePayment` checks for this before calling
`createCustomer` — if the company already has a Stripe customer, the call is skipped
entirely:

```
first initiatePayment  → stripeCustomerReference == null → createCustomer() → store on Company
subsequent retries     → stripeCustomerReference != null → skip createCustomer()
```

The idempotency key (`"cus-{companyId}"`) remains as a safety net for crashes between
the Stripe call and the DB write.

---

## Response Contracts

What Stripe returns and what the adapter keeps.

### POST /v1/customers → `CustomerReference`

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

### POST /v1/payment_intents → `PaymentIntentResult`

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

---

## Webhook Events

All events share the same envelope. The `type` field drives routing; `data.object` is the
full resource at the time the event was created.

```json
{
  "id": "evt_1Pabc123",             ← stored in processed_stripe_events for idempotency
  "type": "payment_intent.succeeded | payment_intent.payment_failed | payment_intent.processing | payment_intent.canceled | payment_intent.requires_action",
  "data": {
    "object": {
      "id": "pi_3Pxyz",             ← used to look up OnboardingSession
      "status": "succeeded | processing | requires_action | canceled",
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
| `payment_intent.processing` | → `ACTIVATION_PROCESSING` | Payment received, async confirmation pending |
| `payment_intent.canceled` | → `ACTIVATION_CANCELED` | Payment canceled |
| `payment_intent.requires_action` | → `ACTION_REQUIRED` | 3DS or redirect required |

---

## Webhook Flow

```
Stripe ──POST──► /webhooks/stripe
                     │
               verify signature         ← Webhook.constructEvent(rawBody, sigHeader, webhookSecret)
                     │                    raw bytes required — do not let Spring deserialize first
               filter event type
                     │                  ← type not in STRIPE_EVENT_TYPES map → return 200 immediately
                     │                    (200 prevents Stripe from retrying an event we'll never handle)
                     │                    TODO: fire unhandled_stripe_event_total{type=...} metric —
                     │                    especially valuable for payment_intent.* types, which could
                     │                    indicate a new Stripe event type we should be handling
               HandlePaymentEventUseCase (@Transactional)
                     │
               recordIfNew(eventId)     ← idempotency guard: duplicate event ID → return 200 immediately
                     │                    atomic INSERT ON CONFLICT DO NOTHING
               load OnboardingSession by paymentIntentId
                     │
               load Company by companyId
                     │
               transition Company status
                     │
               update Company
               (transaction commits — recordIfNew insert + company update are atomic)
```

Signature verification uses `Stripe.constructEvent(rawBody, sigHeader, webhookSecret)`.
**Spring must be configured to provide the raw request body as bytes** — if Spring
deserialises the body first, the signature check fails because the byte representation
changes.

---

## Async Concerns and Race Conditions

### Transaction boundary vs. Stripe API calls

Stripe API calls happen *inside* the `@Transactional` boundary in `InitiatePayment`, but
Stripe does **not** participate in the JDBC transaction. The transaction only covers database
operations. This creates two risks:

**1. Stripe call succeeds, then the transaction rolls back**

`createCustomer` or `createPaymentIntent` succeeds — Stripe objects exist — but then
something causes the transaction to roll back (e.g. a DB write fails). On retry, idempotency
keys ensure the same Stripe objects are returned rather than new ones being created.

**2. Webhook arrives before the transaction commits**

Timeline:
```
t=0    createPaymentIntent() returns → Stripe holds pi_xxx
t=0+ε  Stripe fires webhook for pi_xxx
t=0+ε  webhook handler: findByPaymentIntentId("pi_xxx") → NOT FOUND (not committed yet)
t=0+ε  webhook handler returns 404 → Stripe schedules a retry
t=0+X  InitiatePayment transaction commits → session now has payment_intent_id = pi_xxx
t=retry Stripe retries webhook → findByPaymentIntentId("pi_xxx") → FOUND → processes correctly
```

**Decision:** accept this and rely on Stripe's retry mechanism (retries for up to 72 hours).
In practice, Stripe webhook delivery has enough latency that the transaction almost always
commits first. Called out as a known edge case.

**Production fix if needed:** move Stripe API calls outside the `@Transactional` boundary —
commit the initial DB writes first, then call Stripe, then update the session with the
PaymentIntent ID in a second transaction. This eliminates the race entirely but makes the
use case more complex.

### 3DS — confirmation is fully async

After we return the `clientSecret`, the frontend calls Stripe.js to confirm the payment.
For 3DS, this involves a browser redirect that can take minutes. The backend has no
involvement until Stripe fires the webhook. The company remains in `PENDING_ACTIVATION`
or `ACTION_REQUIRED` throughout — `GET /{sessionId}/status` polling reflects this correctly.

### Stripe webhook retries

Stripe retries webhook delivery for up to 72 hours on non-2xx responses. The same
`payment_intent.succeeded` event can arrive multiple times. The `processed_stripe_events`
idempotency guard handles this: if the event ID is already in the table, return 200
immediately without re-processing.

Without the guard, the second delivery would call `company.paymentSucceeded()` on an
already-`ACTIVE` company — the state machine would throw, the handler would return 500,
and Stripe would keep retrying indefinitely.

---

## Dual-Write Risk

Stripe and PostgreSQL are two separate systems. There is no distributed transaction
spanning both. The failure window is:

```
createPaymentIntent() ──► Stripe object created
        │
     [crash here]
        │
sessionRepository.update() ──► NOT reached — DB has no record of the PaymentIntent
```

**Mitigation:** idempotency keys. On restart and retry, Stripe returns the existing
PaymentIntent rather than creating a new one, so the second attempt completes correctly.

Note: this is a different concern from the `processed_stripe_events` table. Idempotency
keys protect against duplicate Stripe resource creation on *outbound* retries from this
service. `processed_stripe_events` protects against duplicate processing of *inbound*
webhook deliveries from Stripe. Both are needed; neither substitutes for the other.

**Full fix (not implemented):** Transactional Outbox — write the Stripe intent to an
`outbox_events` table in the same DB transaction as the domain write, then have a
poller call Stripe from there. Eliminates the dual-write gap entirely. Called out in
DESIGN.md as a named tradeoff.
