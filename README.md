# Wallet API

[![CI](https://github.com/delciaa/wallet/actions/workflows/ci.yml/badge.svg)](https://github.com/delciaa/wallet/actions/workflows/ci.yml)

A REST API for digital wallets, built with Spring Boot and PostgreSQL. Users can create an account, add money to their wallet, send money to other users and see their transaction history. Admins can freeze wallets and run a reconciliation check on the ledger.

## Features

- Sign-up and login with BCrypt-hashed passwords and JWT access tokens
- One wallet per user, with top-ups and transfers between users
- Idempotent money requests through an `Idempotency-Key` header, so a retried request never runs twice
- Optimistic locking (`@Version`) with automatic retries, which keeps balances correct under concurrent requests
- A double-entry ledger in which every transfer writes two entries that sum to zero
- Paginated transaction history with cursors
- Admin endpoints to freeze and unfreeze wallets and to reconcile balances against the ledger
- Error responses in RFC 9457 Problem Details format

## Tech stack

- Java 21
- Spring Boot 4 (Web, Security, Data JPA, Validation)
- PostgreSQL 17
- Flyway
- Testcontainers and JUnit 5
- GitHub Actions

## Getting started

### Requirements

- Java 21
- Docker

### Setup

1. Copy the example environment file:

   ```bash
   cp .env.example .env
   ```

2. Set `JWT_SECRET` in `.env` to a random value of at least 32 characters. One way to make one:

   ```bash
   openssl rand -base64 48
   ```

3. Start the database:

   ```bash
   docker compose up -d
   ```

4. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

The API runs on `http://localhost:8080`. Flyway creates the tables the first time it starts.

### Configuration

| Variable | Description |
|---|---|
| `JWT_SECRET` | Key used to sign access tokens. Required, at least 32 characters. |
| `ADMIN_EMAIL` | Optional. The account with this email is made an admin when the app starts. Register it first, then restart. |

## API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | — | Create an account |
| `POST` | `/api/auth/login` | — | Get an access token |
| `GET` | `/api/me` | User | Profile and wallet balance |
| `POST` | `/api/wallets/me/top-ups` | User | Add money to your wallet |
| `POST` | `/api/transfers` | User | Send money to another user |
| `GET` | `/api/wallets/me/transactions` | User | Transaction history (`limit`, `cursor`) |
| `GET` | `/api/admin/wallets?email=` | Admin | Find a wallet by email |
| `POST` | `/api/admin/wallets/{id}/freeze` | Admin | Freeze a wallet |
| `POST` | `/api/admin/wallets/{id}/unfreeze` | Admin | Unfreeze a wallet |
| `GET` | `/api/admin/reconciliation` | Admin | Check balances against the ledger |

Authenticated requests need an `Authorization: Bearer <token>` header. Top-ups and transfers also need an `Idempotency-Key` header.

Example:

```http
POST /api/transfers
Authorization: Bearer <token>
Idempotency-Key: 7f0c2d5e-1b2a-4c3d-9e8f-0a1b2c3d4e5f
Content-Type: application/json

{ "recipientEmail": "user@example.com", "amount": 25.00 }
```

A Postman collection with all requests is in [`postman/wallet.postman_collection.json`](postman/wallet.postman_collection.json).

## Running tests

```bash
./mvnw test
```

The tests use Testcontainers to start a real PostgreSQL instance, so Docker must be running.

## Project structure

```
src/main/java/io/github/delciaa/wallet/
├── auth/       registration and login
├── security/   JWT and security configuration
├── user/       users and profile
├── wallet/     wallet entity and balance rules
├── transfer/   top-ups, transfers, ledger and idempotency
├── history/    transaction history
├── admin/      freezing, reconciliation and admin setup
└── common/     shared helpers
```
