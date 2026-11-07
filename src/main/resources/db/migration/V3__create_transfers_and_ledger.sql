-- Every money movement is a transfer between two wallets. A top-up is a transfer
-- from the system's EXTERNAL wallet, which stands for money entering from outside.
CREATE TABLE transfers (
  id UUID PRIMARY KEY,
  type TEXT NOT NULL,
  initiated_by UUID NOT NULL REFERENCES users(id),
  idempotency_key TEXT NOT NULL,
  request_hash TEXT NOT NULL,
  from_wallet_id UUID NOT NULL REFERENCES wallets(id),
  to_wallet_id UUID NOT NULL REFERENCES wallets(id),
  amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  currency CHAR(3) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  -- One key per user: the same key from the same user is the same request
  UNIQUE (initiated_by, idempotency_key),
  CHECK (from_wallet_id <> to_wallet_id)
);

-- Double-entry ledger: each transfer writes one negative and one positive line,
-- so all lines always sum to zero. Lines are never updated or deleted.
CREATE TABLE ledger_entries (
  id UUID PRIMARY KEY,
  transfer_id UUID NOT NULL REFERENCES transfers(id),
  wallet_id UUID NOT NULL REFERENCES wallets(id),
  amount NUMERIC(19,2) NOT NULL CHECK (amount <> 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ledger_entries_wallet_created_idx ON ledger_entries (wallet_id, created_at);
CREATE INDEX ledger_entries_transfer_idx ON ledger_entries (transfer_id);

INSERT INTO wallets (id, user_id, currency, balance, type, status)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'EUR', 0, 'SYSTEM', 'ACTIVE');
