# Testing Guide

The service is live at:

```
https://onboarding-production-e40a.up.railway.app
```

No local setup needed. All flows run entirely via curl.

---

## Prerequisites

You need the Stripe test API key to confirm PaymentIntents. Set it once before running any flow:

```bash
export STRIPE_TEST_KEY=sk_test_...
```

Ask the team for the test key if you don't have it.

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

```bash
export SESSION_ID=<sessionId from response>
```

### 2. Initiate payment

```bash
curl -s -X POST https://onboarding-production-e40a.up.railway.app/api/onboarding/$SESSION_ID/payment \
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

The PaymentIntent ID is the part of `clientSecret` before `_secret_` (e.g. `pi_3Rxxx`).

```bash
export PI_ID=<pi_xxx from clientSecret>
```

Supported country codes: `US`, `GB`, `DE`, `FR`, `CA`, `AU`. Unknown codes fall back to USD and set `pricingWarning`.

### 3. Confirm the payment

```bash
curl -s -X POST https://api.stripe.com/v1/payment_intents/$PI_ID/confirm \
  -u "$STRIPE_TEST_KEY": \
  -d "payment_method=pm_card_visa"
```

Stripe fires the `payment_intent.succeeded` webhook to the Railway service automatically.

### 4. Poll status

```bash
curl -s https://onboarding-production-e40a.up.railway.app/api/onboarding/$SESSION_ID/status
```

```json
{ "status": "ACTIVE" }
```

---

## Flow 2: Card declined → retry → success

Follow steps 1–2 from Flow 1, then:

### 3. Confirm with a declining card

```bash
curl -s -X POST https://api.stripe.com/v1/payment_intents/$PI_ID/confirm \
  -u "$STRIPE_TEST_KEY": \
  -d "payment_method=pm_card_chargeDeclinedInsufficientFunds"
```

### 4. Check status → ACTIVATION_FAILED

```bash
curl -s https://onboarding-production-e40a.up.railway.app/api/onboarding/$SESSION_ID/status
```

```json
{ "status": "ACTIVATION_FAILED" }
```

### 5. Retry payment

```bash
curl -s -X POST https://onboarding-production-e40a.up.railway.app/api/onboarding/$SESSION_ID/retry \
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

```bash
export SESSION_ID=<newSessionId>
export PI_ID=<pi_yyy from new clientSecret>
```

### 6. Confirm retry with a succeeding card

```bash
curl -s -X POST https://api.stripe.com/v1/payment_intents/$PI_ID/confirm \
  -u "$STRIPE_TEST_KEY": \
  -d "payment_method=pm_card_visa"
```

### 7. Check status → ACTIVE

```bash
curl -s https://onboarding-production-e40a.up.railway.app/api/onboarding/$SESSION_ID/status
```

```json
{ "status": "ACTIVE" }
```

---

## Flow 3: Max retries exceeded → REQUIRES_SUPPORT

The service allows a maximum of **3 payment attempts** per company.

Repeat steps 1–5 of Flow 2 three times (reusing the same initial registration each time,
updating `SESSION_ID` and `PI_ID` after each retry). On the 3rd retry call the service
returns HTTP 422:

```json
{ "message": "Maximum payment attempts reached. Please contact support." }
```

Status check confirms:
```json
{ "status": "REQUIRES_SUPPORT" }
```

---

## Flow 4: 3DS authentication (requires frontend)

Confirming a 3DS PaymentIntent requires a browser redirect handled by Stripe.js — it cannot
be completed via curl alone. This flow is intended for frontend integration testing once a
UI is available.

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
