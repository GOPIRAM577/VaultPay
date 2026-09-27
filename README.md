
# PayFlow

> A full-stack payment wallet simulation demonstrating the engineering patterns behind apps like PhonePe, GPay, and Stripe — built for zero cost, deployable in one click, explainable in any interview.

[![Live Demo](https://img.shields.io/badge/Live%20Demo-payflow.vercel.app-6366f1?style=flat-square)](https://payflow.vercel.app)
[![API Docs](https://img.shields.io/badge/API%20Docs-Swagger%20UI-85b7eb?style=flat-square)](https://payflow-api.railway.app/swagger-ui.html)
[![Uptime](https://img.shields.io/uptimerobot/ratio/7/mXXXXXXXX-XXXXXXXXXXXXXXXXXXXXXXXX?style=flat-square&label=Uptime)](https://uptimerobot.com)
[![CI](https://img.shields.io/github/actions/workflow/status/your-username/payflow/backend-ci.yml?style=flat-square&label=CI)](https://github.com/your-username/payflow/actions)
[![Coverage](https://img.shields.io/badge/Coverage-70%25-4ade80?style=flat-square)](./backend/target/site/jacoco/index.html)

**📹 [Watch the 7-minute walkthrough on Loom →](https://loom.com/share/your-link-here)**

---

## What is PayFlow?

PayFlow is not a CRUD app. It is a payment processing simulation that implements the same architectural patterns used by real financial systems — double-entry bookkeeping, payment state machines, idempotency keys, fraud scoring, and the transactional outbox pattern — running entirely on free-tier infrastructure with a live demo anyone can open right now.

The goal is to demonstrate how money moves through a real system, not just that buttons can be clicked.

---

## Demo Accounts

Open the [live app](https://payflow.vercel.app) and log in — no sign-up needed:

| Role | Email | Password |
|---|---|---|
| User A | `demo-a@payflow.com` | `Demo@123` |
| User B | `demo-b@payflow.com` | `Demo@123` |
| Admin | `admin@payflow.com` | `Admin@123` |

**Quick scenarios to try:**
- Send ₹500 from User A to User B — watch the state machine update live via WebSocket
- Send ₹6,000 — triggers fraud rejection with per-rule score breakdown
- Send the same payment twice with the same `Idempotency-Key` header — second request returns cached response
- Admin → freeze User B's wallet → try sending to them → `409 WALLET_FROZEN`

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│              React 18 + TypeScript (Vercel)              │
│         Tailwind · shadcn/ui · Zustand · Recharts        │
└────────────────────┬──────────────────┬──────────────────┘
                     │  REST API        │  WebSocket
                     ▼                  ▼
┌──────────────────────────────────────────────────────────┐
│         Spring Boot 3.2 — Modular Monolith (Railway)     │
│                                                          │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌─────────────┐  │
│  │  auth/  │ │ wallet/ │ │ payment/ │ │   fraud/    │  │
│  └─────────┘ └─────────┘ └──────────┘ └─────────────┘  │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌─────────────┐  │
│  │ ledger/ │ │ outbox/ │ │settlement│ │notification/│  │
│  └─────────┘ └─────────┘ └──────────┘ └─────────────┘  │
│                   audit/ · ratelimit/ · common/          │
└──────────┬──────────────────┬────────────────┬──────────┘
           │                  │                │
           ▼                  ▼                ▼
    ┌─────────────┐   ┌──────────────┐  ┌──────────────┐
    │    Neon     │   │   Upstash    │  │  Groq API    │
    │ PostgreSQL  │   │    Redis     │  │  Resend API  │
    └─────────────┘   └──────────────┘  └──────────────┘
```

Each package inside the monolith maps 1:1 to a microservice boundary in production. Migrating to actual microservices would be a deployment change, not an architectural one — the interfaces are already service-shaped.

---

## Key Engineering Concepts

### Double-Entry Ledger

Every financial event creates exactly two ledger entries that sum to zero. Enforced at the database layer by a PostgreSQL trigger — no application code can bypass it.

```sql
-- Sending ₹500: two rows, always sums to zero
INSERT INTO ledger_entries (wallet_id, entry_type, amount) VALUES
  ('W-001', 'DEBIT',  -500.00),  -- sender
  ('W-002', 'CREDIT', +500.00);  -- receiver

-- Immutability enforced by trigger
CREATE TRIGGER ledger_immutability
BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION prevent_ledger_modification();
```

*Why it matters in an interview:* This is how every bank, wallet, and payment processor stores money. Not a balance column — a permanent, auditable ledger.

---

### Payment State Machine

Every payment moves through a strict, enforced lifecycle. No payment can skip states or move backwards.

```
INITIATED → FRAUD_REVIEW → PROCESSING → COMPLETED
                        ↘              ↘ FAILED
                         REJECTED
```

State transitions are validated in `PaymentStateMachine.java` and pushed to the React frontend in real time via WebSocket. Each transition is also written to the outbox table for downstream processing.

---

### Idempotency Keys

Every payment request requires an `Idempotency-Key: <UUID>` header. The key is stored in Upstash Redis for 24 hours with the serialised response body.

```
Client sends payment → Redis: key exists? → Yes → return cached response (200, not 201)
                                          → No  → process payment, store result in Redis
```

If a client retries due to a network timeout, they get the original response back — no duplicate charge, no duplicate ledger entry. This is exactly how Stripe handles retries.

---

### Transactional Outbox Pattern

Instead of Kafka, PayFlow uses PostgreSQL as a message bus. Payment state changes write an event to `outbox_events` atomically in the same database transaction. A `@Scheduled` poller processes pending events every 2 seconds.

```
Payment completes
  └─→ DB transaction: UPDATE payments SET status = 'COMPLETED'
                       INSERT INTO outbox_events (event_type, payload) VALUES ('PAYMENT_COMPLETED', ...)
          ↓ (same transaction — atomic)
  └─→ OutboxPoller picks up event
          ├─→ Write ledger entries
          ├─→ Send email receipt via Resend API
          └─→ Push WebSocket status update to frontend
```

*Interview talking point:* In production, replace the poller with a Kafka consumer group. The event schema stays identical — it's a deployment change, not an architectural one.

---

### Concurrent Transfer Safety

Wallet balances use `SELECT ... FOR UPDATE` to prevent concurrent overdraft. If two threads try to drain the same wallet simultaneously, the second one waits for the first to commit, then reads the updated balance.

```java
// Prevents concurrent overdraft — one of the most important tests in the project
@Test
void concurrent_transfers_should_not_allow_overdraft() throws Exception {
    // Wallet starts at ₹1,000
    // 5 threads each try to send ₹800 simultaneously
    // Exactly 1 succeeds, 4 fail with INSUFFICIENT_FUNDS
    // Final balance: ₹200, never negative
    // Ledger entries sum to zero
}
```

---

### Fraud Pre-Screening

A rule-based scoring engine computes a risk score (0.0–1.0) before every payment is processed:

| Rule | Score | Rationale |
|---|---|---|
| > 5 transactions in 1 hour | +0.30 | Velocity anomaly |
| Amount > ₹5,000 | +0.20 | Large single transfer |
| Wallet age < 24 hours | +0.15 | New account risk |
| First-time recipient | +0.10 | Unknown payee |
| Night-time (11pm–5am IST) | +0.05 | Off-hours anomaly |

- Score `< 0.4` → auto-approve
- Score `0.4–0.69` → Groq AI generates a narrative explanation
- Score `≥ 0.7` → hard reject

The scoring architecture is identical to production — a pipeline that emits a float between 0 and 1. Swapping the rule engine for a scikit-learn model is a single-class substitution behind a `FraudScoringStrategy` interface.

---

### Rate Limiting

Per-user sliding window rate limiting backed by Upstash Redis. Payment initiation is limited to 30 requests per user per minute. Auth endpoints are limited to 10 requests per IP per minute to prevent brute force.

```java
// Redis key: rate:{userId}:{endpoint}:{windowMinute}
// Returns 429 RATE_LIMIT_EXCEEDED with Retry-After header when exceeded
```

---

### Cursor-Based Pagination

Transaction history uses cursor-based pagination, not OFFSET. OFFSET requires the database to scan and discard the first N rows on every page request — at 10 million ledger entries, page 500 would scan 10 million rows to return 20.

```
GET /wallets/me/transactions?cursor=<lastEntryId>&limit=20
```

The cursor is the opaque `id` of the last seen entry. The database reads from a known B-tree index position — O(1) per page regardless of dataset size.

---

## Tech Stack

| Layer | Technology | Free Tier |
|---|---|---|
| Frontend | React 18 + TypeScript + Tailwind + shadcn/ui | Vercel — unlimited |
| Backend | Spring Boot 3.2 (Java 21) | Railway — ~500hr/month |
| Database | Neon PostgreSQL | 512 MB free |
| Cache | Upstash Redis | 10,000 commands/day |
| Fraud AI | Groq API (llama-3.1-8b-instant) | Generous free tier |
| Email | Resend API | 3,000 emails/month |
| CI/CD | GitHub Actions + Vercel + Railway | Free for public repos |
| Load Testing | k6 Cloud | 50 VUs, 1 test/month |
| API Docs | Springdoc OpenAPI (Swagger UI) | Built-in |
| Monitoring | UptimeRobot | Free |

**Total monthly cost: ₹0** at demo scale.

---

## API Endpoints

Full interactive documentation at [`/swagger-ui.html`](https://payflow-api.railway.app/swagger-ui.html) with pre-filled example payloads.

**Auth**
```
POST /auth/register         Register + wallet provisioned automatically
POST /auth/login            JWT login (24hr expiry)
```

**Wallet**
```
GET  /wallets/me            Wallet details
GET  /wallets/me/balance    Cached balance (Redis, 30s TTL)
GET  /wallets/me/transactions?cursor={id}&limit=20
GET  /wallets/me/qr         QR code PNG for receive link
POST /wallets/topup         Simulate wallet top-up
```

**Payments**
```
POST /payments/initiate     Create payment intent
                            Header: Idempotency-Key: <UUID v4>
                            Body: { recipientWalletId|phone|upiHandle, amount, note }
GET  /payments/{id}/status  Poll payment state
WS   /ws/payments/{id}      Real-time state machine updates
```

**Admin** (JWT role: ADMIN)
```
GET    /admin/wallets
PATCH  /admin/wallets/{id}/status       Freeze / unfreeze
GET    /admin/fraud/alerts              Flagged payments with AI narrative
GET    /admin/settlements/{date}/report.csv
GET    /admin/audit-logs
GET    /admin/reconciliation
```

---

## Performance

Load tested with k6 Cloud at 20 TPS sustained for 60 seconds.

| Metric | Result |
|---|---|
| P95 latency (payment initiation) | < 500ms |
| P95 latency (balance read, cached) | < 100ms |
| Error rate at 20 TPS | < 1% |
| Idempotency correctness | 100% |
| Ledger reconciliation mismatches | 0 |

[View full k6 report →](./k6/reports/load-test-results.png)

The bottleneck at 20 TPS is the HikariCP connection pool on a single Railway instance. To scale: PgBouncer for connection pooling, read replicas for balance queries, and ledger table partitioned by wallet ID hash.

---

## Project Structure

```
payflow/
├── backend/
│   ├── Dockerfile
│   ├── railway.toml                      ← Deployment config (IaC equivalent)
│   └── src/main/java/com/payflow/
│       ├── auth/
│       ├── wallet/
│       ├── payment/
│       ├── ledger/
│       ├── fraud/
│       ├── outbox/
│       ├── settlement/
│       ├── notification/
│       ├── audit/
│       ├── ratelimit/
│       └── common/
├── frontend/
│   ├── vercel.json
│   └── src/
├── db/
│   └── migrations/                       ← Flyway versioned SQL
│       ├── V1__init.sql
│       ├── V2__audit_log.sql
│       └── V3__settlement.sql
├── k6/
│   ├── smoke-test.js
│   ├── load-test.js
│   └── reports/
│       └── load-test-results.png
└── .github/
    └── workflows/
        ├── backend-ci.yml                ← Build → test → coverage gate → deploy
        └── frontend-ci.yml
```

---

## Running Locally

**Prerequisites:** Java 21, Node 20, Docker (for Testcontainers)

```bash
# Clone
git clone https://github.com/your-username/payflow.git
cd payflow

# Backend
cd backend
cp .env.example .env          # fill in your free-tier keys
./mvnw spring-boot:run

# Frontend
cd ../frontend
npm install
npm run dev
```

**Environment variables needed** (all free-tier, takes ~15 min to set up):

| Variable | Where to get it |
|---|---|
| `DATABASE_URL` | [neon.tech](https://neon.tech) → create project |
| `REDIS_URL` | [upstash.com](https://upstash.com) → create Redis |
| `GROQ_API_KEY` | [console.groq.com](https://console.groq.com) → API keys |
| `RESEND_API_KEY` | [resend.com](https://resend.com) → API keys |
| `JWT_SECRET` | any random 64-char string |

---

## Running Tests

```bash
cd backend

# Unit + integration tests (Testcontainers spins up PostgreSQL + Redis automatically)
./mvnw test

# Coverage report — opens in browser
./mvnw jacoco:report
open target/site/jacoco/index.html

# Key test to look at:
# src/test/java/com/payflow/wallet/WalletServiceTest.java
# → concurrent_transfers_should_not_allow_overdraft()
```

---

## Why These Architectural Choices?

**Modular monolith over microservices:** Each package has explicit service-shaped interfaces, but runs in one process. Zero infra cost, zero network overhead between packages, and far more honest to explain than fake microservices on a laptop. Shopify and Stack Overflow run on modular monoliths.

**Outbox pattern over Kafka:** Same architectural guarantee — at-least-once delivery, atomic state + event write — at zero infrastructure cost. Replacing the poller with a Kafka consumer is a deployment change, not an architectural one.

**Rule-based fraud scoring over ML:** Training a classifier requires labelled fraud data. The architecture is identical to production — a pipeline that emits a float. Swapping rules for a model is a single-class change behind a `FraudScoringStrategy` interface.

**Neon PostgreSQL over MongoDB:** The financial domain is inherently relational. Double-entry ledgers, wallet balances, and payment relationships are cleaner in SQL, and PostgreSQL's trigger system enforces ledger immutability at the DB layer — no ORM can bypass it.

**Cursor pagination over OFFSET:** OFFSET degrades linearly with dataset size. Cursor pagination reads from a known index position — O(1) per page at any scale.

---


---
