# Testing Guide

The service is live at:

```
https://onboarding-production-e40a.up.railway.app
```

No local setup needed. Use curl, Postman, or any HTTP client.

---

## Stripe test cards

| Card number | Scenario |
|---|---|
| `4242 4242 4242 4242` | Payment succeeds immediately |
| `4000 0000 0000 9995` | Card declined (insufficient funds) |
| `4000 0025 0000 3155` | 3DS authentication required (needs frontend) |

Use any future expiry date, any 3-digit CVC, any postal code.

---

## Flow 1: Successful onboarding

### 1. Register a company

```bash
curl -s -X POST https://onboarding-production-e40a.up.railway.app/api/onboarding/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Acme Corp",
    "adminEmail": "admin@acme.com",
    "adminFirstName": "Jane",
    "adminLastName": "Doe"
  }'
```

Response:
```json
{
  "sessionId": "a1b2c3d4-...",
  "companyId": "e5f6g7h8-..."
}
```

Save the `sessionId` — you need it for all subsequent calls.

### 2. Initiate payment

```bash
curl -s -X POST https://onboarding-production-e40a.up.railway.app/api/onboarding/{sessionId}/payment \
  -H "Content-Type: application/json" \
  -d '{ "ipCountry": "US" }'
```

Response:
```json
{
  "clientSecret": "pi_xxx_secret_xxx",
  "pricingWarning": null
}
```

Save the `clientSecret`. If `ipCountry` is not in the pricing table, `pricingWarning` will contain a message and the price falls back to USD.

Supported country codes: `US`, `GB`, `DE`, `FR`, `CA`, `AU`.

### 3. Confirm the payment via Stripe Dashboard

1. Go to [Stripe Dashboard → PaymentIntents](https://dashboard.stripe.com/test/payments)
2. Find the PaymentIntent that was just created
3. Click **Confirm** and enter card `4242 4242 4242 4242`
4. Stripe fires the `payment_intent.succeeded` webhook to the Railway service automatically

### 4. Poll status

```bash
curl -s https://onboarding-production-e40a.up.railway.app/api/onboarding/{sessionId}/status
```

Response:
```json
{ "status": "ACTIVE" }
```

---

## Flow 2: Card declined → retry → success

Follow steps 1–2 from Flow 1, then:

### 3. Confirm with a declining card

In the Stripe Dashboard, confirm the PaymentIntent with card `4000 0000 0000 9995`.

### 4. Check status → ACTIVATION_FAILED

```bash
curl -s https://onboarding-production-e40a.up.railway.app/api/onboarding/{sessionId}/status
```

```json
{ "status": "ACTIVATION_FAILED" }
```

### 5. Retry payment

```bash
curl -s -X POST https://onboarding-production-e40a.up.railway.app/api/onboarding/{sessionId}/retry \
  -H "Content-Type: application/json" \
  -d '{ "ipCountry": "US" }'
```

Response:
```json
{
  "newSessionId": "b2c3d4e5-...",
  "clientSecret": "pi_yyy_secret_yyy",
  "pricingWarning": null
}
```

Use the `newSessionId` for all subsequent status checks.

### 6. Confirm retry with a succeeding card

In the Stripe Dashboard, confirm the new PaymentIntent with card `4242 4242 4242 4242`.

### 7. Check status → ACTIVE

```bash
curl -s https://onboarding-production-e40a.up.railway.app/api/onboarding/{newSessionId}/status
```

```json
{ "status": "ACTIVE" }
```

---

## Flow 3: Max retries exceeded → REQUIRES_SUPPORT

The service allows a maximum of **3 payment attempts** per company.

Repeat the decline flow (steps 1–5 of Flow 2) three times using the same original company registration, each time using the `newSessionId` returned from the previous retry.

On the 3rd retry attempt, the retry call itself will return HTTP 422 and the company transitions to `REQUIRES_SUPPORT`:

```json
{ "message": "Maximum payment attempts reached. Please contact support." }
```

Status check confirms:
```json
{ "status": "REQUIRES_SUPPORT" }
```

---

## Flow 4: 3DS authentication (requires frontend)

Confirming a 3DS PaymentIntent (`4000 0025 0000 3155`) requires a browser redirect handled
by Stripe.js — it cannot be completed via curl or the Stripe Dashboard alone. This flow is
intended for frontend integration testing once a UI is available.

Expected status progression: `PENDING_ACTIVATION` → `ACTION_REQUIRED` (while 3DS is pending)
→ `ACTIVE` (once authenticated).

---

## Validation errors

Missing or invalid fields return HTTP 400:

```bash
curl -s -X POST https://onboarding-production-e40a.up.railway.app/api/onboarding/register \
  -H "Content-Type: application/json" \
  -d '{ "companyName": "Acme" }'
```

```json
{ "message": "Validation failed: adminEmail: must not be blank; ..." }
```

`ipCountry` must be exactly two uppercase letters (`"US"`, `"GB"`, etc.). Any other format returns HTTP 400.
