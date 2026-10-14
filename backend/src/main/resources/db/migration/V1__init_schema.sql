CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    upi_handle VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- WALLETS
CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT balance_non_negative CHECK (balance >= 0)
);

-- PAYMENTS
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_wallet_id UUID NOT NULL REFERENCES wallets(id),
    receiver_wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    fraud_score NUMERIC(3,2),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- LEDGER ENTRIES
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID REFERENCES payments(id),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    seq BIGSERIAL NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- OUTBOX EVENTS
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- FRAUD ASSESSMENTS
CREATE TABLE fraud_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID REFERENCES payments(id),
    risk_score NUMERIC(3,2) NOT NULL,
    rules_triggered JSONB NOT NULL,
    decision VARCHAR(20) NOT NULL,
    ai_narrative TEXT,
    assessed_at TIMESTAMP DEFAULT NOW()
);

-- RECONCILIATION ALERTS
CREATE TABLE reconciliation_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    expected_balance NUMERIC(12,2),
    actual_balance NUMERIC(12,2),
    discrepancy NUMERIC(12,2),
    detected_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);

-- AUDIT LOGS
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- SETTLEMENT REPORTS
CREATE TABLE settlement_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_date DATE NOT NULL,
    wallet_id UUID REFERENCES wallets(id),
    total_credits NUMERIC(12,2) DEFAULT 0.00,
    total_debits NUMERIC(12,2) DEFAULT 0.00,
    net_position NUMERIC(12,2) DEFAULT 0.00,
    generated_at TIMESTAMP DEFAULT NOW()
);

-- INDEXES
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

CREATE INDEX idx_payments_sender_wallet
ON payments(sender_wallet_id);

CREATE INDEX idx_payments_receiver_wallet
ON payments(receiver_wallet_id);

CREATE INDEX idx_payments_status
ON payments(status);

CREATE INDEX idx_ledger_entries_wallet
ON ledger_entries(wallet_id);

CREATE INDEX idx_ledger_entries_payment
ON ledger_entries(payment_id);

CREATE INDEX idx_outbox_status
ON outbox_events(status);

CREATE INDEX idx_fraud_payment
ON fraud_assessments(payment_id);

CREATE INDEX idx_audit_actor
ON audit_logs(actor_id);

CREATE INDEX idx_settlement_wallet
ON settlement_reports(wallet_id);