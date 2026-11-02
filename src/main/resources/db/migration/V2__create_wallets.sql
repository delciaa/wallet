CREATE TABLE wallets (
  id UUID PRIMARY KEY,
  user_id UUID UNIQUE REFERENCES users(id),  -- NULL for system accounts
  currency CHAR(3) NOT NULL DEFAULT 'EUR',
  balance NUMERIC(19,2) NOT NULL DEFAULT 0,
  type TEXT NOT NULL DEFAULT 'USER',
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (type <> 'USER' OR balance >= 0)
);
