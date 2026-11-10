-- =====================================================================
-- BFSI Payment System: reference schema (PostgreSQL 14+)
-- The services create these tables automatically (spring.jpa.hibernate.ddl-auto=update).
-- Use this file for review, DBA hand-off, or to run with ddl-auto=validate.
-- Each section targets its own database: \c <db> before running.
-- =====================================================================

-- ---------- auth_db ----------
-- \c auth_db
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,                 -- BCrypt
    role           VARCHAR(20)  NOT NULL,                 -- CUSTOMER | ADMIN
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- customer_db ----------
-- \c customer_db
CREATE TABLE customers (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL UNIQUE,          -- logical FK -> auth_db.users.id
    full_name      VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(15)  NOT NULL,
    dob            DATE,
    pan            VARCHAR(10),
    aadhaar_last4  VARCHAR(4),                              -- full Aadhaar is never stored
    address        VARCHAR(500),
    kyc_status     VARCHAR(20)  NOT NULL DEFAULT 'NOT_SUBMITTED'
                   CHECK (kyc_status IN ('NOT_SUBMITTED','PENDING','VERIFIED','REJECTED')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(20)   NOT NULL UNIQUE,
    user_id         BIGINT        NOT NULL,
    account_type    VARCHAR(20)   NOT NULL DEFAULT 'SAVINGS',
    ifsc            VARCHAR(11)   NOT NULL DEFAULT 'BFSI0000001',
    balance         NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_accounts_user ON accounts(user_id);

-- ---------- payment_db ----------
-- \c payment_db
CREATE TABLE payments (
    id               BIGSERIAL PRIMARY KEY,
    txn_id           VARCHAR(50)   NOT NULL UNIQUE,       -- TXN-<MODE>-<yyyyMMddHHmmss>-<6 chars>
    user_id          BIGINT        NOT NULL,
    user_email       VARCHAR(150),
    from_account     VARCHAR(20)   NOT NULL,
    to_account       VARCHAR(100)  NOT NULL,              -- account no / VPA / merchant
    amount           NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    mode             VARCHAR(10)   NOT NULL CHECK (mode IN ('NEFT','UPI','CARD')),
    status           VARCHAR(10)   NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    failure_reason   VARCHAR(255),
    ifsc             VARCHAR(11),
    card_last4       VARCHAR(4),
    remarks          VARCHAR(255),
    idempotency_key  VARCHAR(64),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_payment_idem UNIQUE (user_id, idempotency_key)
);
CREATE INDEX idx_payment_user ON payments(user_id);

-- ---------- ledger_db ----------
-- \c ledger_db
CREATE TABLE ledger_entries (                              -- append-only
    id              BIGSERIAL PRIMARY KEY,
    txn_id          VARCHAR(50)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    account_number  VARCHAR(20)   NOT NULL,
    entry_type      VARCHAR(10)   NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount          NUMERIC(19,2) NOT NULL,
    balance_after   NUMERIC(19,2),
    mode            VARCHAR(10),
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_ledger_user ON ledger_entries(user_id);
CREATE INDEX idx_ledger_txn  ON ledger_entries(txn_id);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor       VARCHAR(255) NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    details     VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Optional hardening: make the ledger tamper-resistant at DB level
-- REVOKE UPDATE, DELETE ON ledger_entries, audit_logs FROM bfsi;

-- ---------- notification_db ----------
-- \c notification_db
CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    channel     VARCHAR(20)  NOT NULL DEFAULT 'IN_APP',
    message     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
