# VaultPay

> A full-stack digital payment wallet simulation demonstrating secure payment processing, wallet management, fraud detection, transaction tracking, and modern full-stack engineering practices.

---

## What is VaultPay?

VaultPay is a full-stack payment wallet simulation designed to demonstrate how modern digital payment platforms handle users, wallets, payments, transactions, fraud detection, authentication, and administrative operations.

Unlike a basic CRUD application, VaultPay implements several real-world backend engineering concepts including:

* Secure JWT-based authentication
* Digital wallet management
* Payment processing
* Double-entry ledger concepts
* Payment state management
* Idempotency handling
* Fraud risk scoring
* Transaction reconciliation
* Audit logging
* Rate limiting
* Redis-based caching
* WebSocket-based real-time updates
* Transactional outbox processing

The project combines a **Spring Boot backend** with a **React + TypeScript frontend** to provide a complete payment-wallet experience.

---

## Key Features

### User Authentication

* User registration
* User login
* JWT-based authentication
* Protected routes
* Role-based authorization
* Account security controls

### Digital Wallet

* Wallet creation
* Wallet balance management
* Wallet status management
* Wallet top-up simulation
* UPI handle support
* QR-based receiving functionality

### Payments

* Send money between wallets
* Payment initiation
* Payment status tracking
* Payment state transitions
* Idempotent payment requests
* Real-time payment updates through WebSockets

### Fraud Detection

VaultPay includes a rule-based fraud scoring engine that evaluates transactions before processing.

The system considers factors such as:

* Transaction amount
* Transaction frequency
* Wallet age
* Recipient history
* Transaction timing

The fraud engine generates a risk score that can be used to approve, review, or reject suspicious transactions.

### Admin Dashboard

Administrators can:

* View users and wallets
* Freeze and unfreeze wallets
* Monitor fraud alerts
* View audit logs
* View settlement information
* Monitor system health
* Perform reconciliation checks

---

## Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│              React + TypeScript Frontend                    │
│        Vite · Tailwind CSS · Zustand · WebSocket            │
└────────────────────────────┬────────────────────────────────┘
                             │
                       REST API / WebSocket
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot Backend                         │
│                     Modular Monolith                        │
│                                                             │
│   ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐    │
│   │  Auth   │  │ Wallet  │  │ Payment  │  │  Fraud   │    │
│   └─────────┘  └─────────┘  └──────────┘  └──────────┘    │
│                                                             │
│   ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐    │
│   │ Ledger  │  │ Outbox  │  │Settlement│  │  Audit   │    │
│   └─────────┘  └─────────┘  └──────────┘  └──────────┘    │
│                                                             │
│              Security · Rate Limiting · Common              │
└───────────────┬──────────────────┬──────────────────────────┘
                │                  │
                ▼                  ▼
        ┌──────────────┐    ┌──────────────┐
        │  PostgreSQL  │    │    Redis     │
        │   Database   │    │ Cache/State  │
        └──────────────┘    └──────────────┘
```

The application follows a modular-monolith architecture where different business capabilities are separated into dedicated controllers, services, repositories, entities, and supporting components.

---

## Key Engineering Concepts

### 1. Payment State Machine

Every payment follows a controlled lifecycle rather than jumping directly from creation to completion.

```text
INITIATED
    │
    ▼
FRAUD REVIEW
    │
    ├──────────────► REJECTED
    │
    ▼
PROCESSING
    │
    ├──────────────► FAILED
    │
    ▼
COMPLETED
```

The payment state transitions are handled by:

```text
PaymentStateMachine.java
```

This prevents invalid state transitions and provides predictable payment processing.

---

### 2. Double-Entry Ledger

VaultPay uses ledger entries to maintain an auditable record of financial transactions.

For example, when ₹500 is transferred:

```text
Sender Wallet       → DEBIT   ₹500
Receiver Wallet     → CREDIT  ₹500
```

The two entries balance each other:

```text
-₹500 + ₹500 = ₹0
```

This approach provides a more reliable financial transaction history than simply updating a wallet balance.

---

### 3. Idempotency

Payment APIs can receive duplicate requests because of network retries or client-side issues.

VaultPay includes idempotency handling to prevent the same payment from being processed multiple times.

```text
Client
  │
  │ Payment + Idempotency Key
  ▼
Backend
  │
  ├── Key already exists?
  │        │
  │        └── YES → Return previous response
  │
  └── NO → Process payment
             │
             └── Store result
```

This helps prevent duplicate transactions.

---

### 4. Fraud Scoring

Before a payment is processed, VaultPay evaluates transaction risk using a rule-based fraud scoring engine.

Example factors include:

| Risk Factor          | Description                                           |
| -------------------- | ----------------------------------------------------- |
| Transaction Amount   | Large transfers can increase risk                     |
| Transaction Velocity | Multiple transactions in a short period               |
| Wallet Age           | Newly created wallets may receive additional scrutiny |
| Recipient History    | First-time recipients can increase risk               |
| Transaction Time     | Unusual transaction times can contribute to risk      |

The scoring engine produces a risk value that can be used by the payment processing pipeline.

---

### 5. Transactional Outbox

VaultPay includes an outbox mechanism for handling events generated during payment processing.

```text
Payment Processing
       │
       ▼
Database Transaction
       │
       ├── Update Payment
       │
       └── Create Outbox Event
                  │
                  ▼
             Outbox Poller
                  │
        ┌─────────┼─────────┐
        ▼         ▼         ▼
      Ledger    Email    WebSocket
```

The outbox approach helps ensure that important events are not lost when processing transactions.

---

### 6. Concurrent Transfer Protection

Payment systems must handle multiple requests attempting to modify the same wallet simultaneously.

VaultPay uses database-level transaction handling and locking mechanisms to reduce the possibility of concurrent overdrafts.

Conceptually:

```text
Wallet Balance: ₹1000

Request 1 → Send ₹800
Request 2 → Send ₹800

Only one transaction should successfully consume
the available ₹1000 balance.

Final Balance → ₹200
```

This is particularly important for financial applications where race conditions can cause incorrect balances.

---

### 7. Rate Limiting

VaultPay includes rate-limiting mechanisms to protect APIs from excessive requests.

The backend contains dedicated rate-limiting and security filters for controlling request frequency.

This helps protect sensitive endpoints such as:

* Authentication
* Payment initiation
* Wallet operations

---

### 8. Cursor-Based Transaction History

Transaction history is designed to support scalable retrieval using cursor-based pagination.

```text
GET /wallets/me/transactions?cursor=<id>&limit=20
```

Instead of repeatedly scanning earlier records using a large `OFFSET`, the cursor identifies the last retrieved transaction and allows the database to continue from that point.

---

## Tech Stack

| Layer                   | Technology         |
| ----------------------- | ------------------ |
| Frontend                | React + TypeScript |
| Build Tool              | Vite               |
| Styling                 | Tailwind CSS       |
| State Management        | Zustand            |
| Backend                 | Spring Boot        |
| Language                | Java               |
| Database                | PostgreSQL         |
| Cache                   | Redis              |
| Authentication          | JWT                |
| API Communication       | REST               |
| Real-Time Communication | WebSocket          |
| Database Migration      | Flyway             |
| Build Tool              | Maven              |
| Containerization        | Docker             |
| Version Control         | Git + GitHub       |

---

## Backend Architecture

```text
backend/
└── src/
    ├── main/
    │   ├── java/com/payflow/backend/
    │   │
    │   ├── common/
    │   ├── config/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── exception/
    │   ├── filter/
    │   ├── repository/
    │   ├── security/
    │   └── service/
    │
    └── resources/
        ├── application.properties
        ├── application.yml
        ├── db/migration/
        └── logback-spring.xml
```

### Major Backend Components

| Component          | Responsibility                             |
| ------------------ | ------------------------------------------ |
| Controllers        | REST API endpoints                         |
| Services           | Business logic                             |
| Repositories       | Database access                            |
| Entities           | Persistent data models                     |
| DTOs               | API request/response models                |
| Security           | JWT authentication and authorization       |
| Filters            | Idempotency, rate limiting, authentication |
| Fraud Engine       | Transaction risk evaluation                |
| Ledger Service     | Financial transaction records              |
| Outbox Poller      | Event processing                           |
| Settlement Service | Settlement processing                      |
| Audit Service      | System activity tracking                   |

---

## Frontend Architecture

```text
frontend/
├── src/
│   ├── components/
│   │   ├── admin/
│   │   ├── Navbar.tsx
│   │   ├── ProtectedRoute.tsx
│   │   ├── QrCodeWidget.tsx
│   │   ├── RecentTransactions.tsx
│   │   ├── SendMoneyModal.tsx
│   │   ├── TopUp.tsx
│   │   └── WalletBalance.tsx
│   │
│   ├── hooks/
│   ├── lib/
│   ├── pages/
│   ├── store/
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
│
├── public/
├── package.json
├── vite.config.ts
└── tailwind.config.js
```

---

## API Endpoints

### Authentication

```text
POST /auth/register
POST /auth/login
```

### Wallet

```text
GET  /wallets/me
GET  /wallets/me/balance
GET  /wallets/me/transactions
GET  /wallets/me/qr
POST /wallets/topup
```

### Payments

```text
POST /payments/initiate
GET  /payments/{id}/status
WS   /ws/payments/{id}
```

### Admin

```text
GET    /admin/wallets
PATCH  /admin/wallets/{id}/status
GET    /admin/fraud/alerts
GET    /admin/settlements/{date}/report.csv
GET    /admin/audit-logs
GET    /admin/reconciliation
```

---

## Project Structure

```text
VaultPay/
│
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   │
│   └── src/
│       ├── main/
│       │   ├── java/com/payflow/backend/
│       │   │   ├── common/
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── dto/
│       │   │   ├── entity/
│       │   │   ├── exception/
│       │   │   ├── filter/
│       │   │   ├── repository/
│       │   │   ├── security/
│       │   │   └── service/
│       │   │
│       │   └── resources/
│       │       ├── db/migration/
│       │       ├── application.properties
│       │       ├── application.yml
│       │       └── logback-spring.xml
│       │
│       └── test/
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── lib/
│   │   ├── pages/
│   │   ├── store/
│   │   ├── App.tsx
│   │   └── main.tsx
│   │
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.ts
│   └── tailwind.config.js
│
├── .gitignore
└── README.md
```

---

## Running Locally

### Prerequisites

Make sure the following are installed:

* Java
* Maven
* Node.js
* npm
* PostgreSQL
* Redis

### Clone the Repository

```bash
git clone https://github.com/GOPIRAM577/VaultPay.git
cd VaultPay
```

### Backend

```bash
cd backend
```

Configure the required environment variables and database settings, then run:

```bash
./mvnw spring-boot:run
```

On Windows:

```cmd
mvnw.cmd spring-boot:run
```

### Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend will then be available through the Vite development server.

---

## Running Tests

Navigate to the backend:

```bash
cd backend
```

Run the test suite:

```bash
./mvnw test
```

On Windows:

```cmd
mvnw.cmd test
```

The project contains tests covering important backend functionality including:

* Application startup
* Payment integration
* Fraud scoring
* Payment state transitions
* Outbox processing

---

## Security Considerations

VaultPay includes several mechanisms designed to demonstrate secure payment-system engineering:

* JWT authentication
* Protected API routes
* Role-based authorization
* Password-based authentication
* Idempotency handling
* Rate limiting
* Fraud detection
* Audit logging
* Wallet status controls
* Transaction validation

Sensitive configuration such as database credentials, JWT secrets, and API keys should be supplied through environment variables rather than committed to Git.

---

## Why These Architectural Choices?

### Modular Monolith

VaultPay uses a modular-monolith structure instead of unnecessarily splitting the application into multiple microservices.

This provides:

* Lower infrastructure complexity
* Easier local development
* Faster communication between modules
* Clear separation of responsibilities
* A simpler deployment model

The individual modules can later be separated into services if the system grows.

### PostgreSQL

A relational database is appropriate for a payment system because wallets, payments, users, ledger entries, and audit records have strong relationships and transactional requirements.

### Redis

Redis can be used for:

* Caching
* Idempotency
* Rate limiting
* Short-lived payment state
* Fast-access application data

### WebSockets

WebSockets allow payment state changes to be communicated to the frontend in real time rather than requiring the client to repeatedly refresh the payment status.

### Rule-Based Fraud Detection

A rule-based fraud engine makes the system transparent and easy to explain.

Each rule contributes to the overall risk assessment, making it possible to understand why a transaction was flagged.

---

## Interview Talking Points

VaultPay provides several useful topics to discuss during technical interviews:

**Backend**

* Spring Boot architecture
* REST API design
* JWT authentication
* Role-based authorization
* Database transactions
* Repository pattern
* Service-layer architecture

**System Design**

* Payment state machines
* Idempotency
* Double-entry ledger
* Transactional outbox
* Concurrent transaction handling
* Rate limiting
* Caching
* WebSockets

**Frontend**

* React component architecture
* TypeScript
* State management with Zustand
* Protected routes
* REST API integration
* Real-time WebSocket updates

**Database**

* PostgreSQL
* Relational data modeling
* Flyway migrations
* Transaction consistency
* Ledger entries
* Audit logging

---

## Future Improvements

Possible future extensions include:

* Machine-learning-based fraud detection
* Kafka-based event processing
* Payment gateway integration
* Real UPI integration
* Advanced analytics dashboard
* Distributed tracing
* Prometheus/Grafana monitoring
* Automated CI/CD deployment
* Kubernetes deployment
* Microservice migration

---

## Author

**VaultPay — Full-Stack Digital Payment Wallet Simulation**

Built using modern Java backend and React frontend technologies to demonstrate real-world payment-system architecture and engineering practices.

---

## License

This project is intended for educational, demonstration, and portfolio purposes.
