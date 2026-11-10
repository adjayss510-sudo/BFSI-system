# BFSI Payment System

Spring Boot 3 / Spring Cloud microservices with a React (Vite) frontend for customer onboarding, KYC, NEFT / UPI / Card payments, a ledger with audit trail, and an admin dashboard.

```
                         ┌────────────────────────────┐
 React (5173) ─────────► │  API Gateway (8080)        │  validates JWT, injects X-User-* headers
                         └──────┬─────────────────────┘
        ┌───────────┬───────────┼────────────┬─────────────────┐
        ▼           ▼           ▼            ▼                 ▼
   auth-service customer-svc payment-svc  ledger-svc    notification-svc
     (8081)       (8082)      (8083)       (8084)          (8085)
     auth_db    customer_db  payment_db   ledger_db     notification_db
        │ Feign      ▲            │ Feign ─────┴──────────────┘
        └────────────┴────────────┘ (customer: create profile / transfer funds)
                 Eureka discovery server (8761)       PostgreSQL (5432)
```

| Service | Port | Responsibility |
|---|---|---|
| discovery-server | 8761 | Eureka registry |
| api-gateway | 8080 | Single entry point (`/api/**`), CORS, JWT validation |
| auth-service | 8081 | Registration, login, JWT issue, seeded admin |
| customer-service | 8082 | KYC profile, accounts, atomic fund transfer (core-banking stand-in) |
| payment-service | 8083 | NEFT/UPI/Card validation + limits, transaction IDs, idempotency, orchestration |
| ledger-service | 8084 | Append-only debit/credit ledger + audit trail |
| notification-service | 8085 | Payment alerts (stored + logged; plug SMS/email here) |

## Prerequisites
Docker with Compose v2 (Option A), or Java 17+, Maven 3.9+, Node 18+ and Docker for PostgreSQL (Option B).

## Run it

### Option A: everything in Docker (recommended)
```bash
docker compose up -d --build      # first build takes a few minutes (Maven + npm)
docker compose ps                 # wait until all containers are Up (about 60 s for Eureka registration)
docker compose logs -f payment-service    # tail one service
docker compose down               # stop (add -v to also wipe the database volume)
```
| URL | What |
|---|---|
| http://localhost:5173 | React frontend (nginx) |
| http://localhost:8080/api | API gateway (use this base URL in Postman) |
| http://localhost:8761 | Eureka dashboard |
| localhost:5432 | PostgreSQL (`bfsi` / `bfsi123`) |

Notes:
- Start-up order is handled with health checks: Postgres and Eureka come up first, then the services, then the frontend.
- Needs roughly 3 GB of free RAM for the 7 JVMs.
- Secrets can be overridden with a `.env` file next to `docker-compose.yml` (`DB_PASSWORD`, `JWT_SECRET`, `INTERNAL_SECRET`). Change `DB_PASSWORD` *before* the first start, because Postgres only reads it when it creates an empty volume.
- `VITE_API_URL` is baked into the frontend image and is called by the *browser*, so it must be a host-reachable address. If you deploy on another host, set it and rebuild: `VITE_API_URL=https://my-host/api docker compose build frontend`. You must also add that origin to the gateway's CORS list in `api-gateway/src/main/resources/application.yml`.
- `db/init.sql` (creating the five databases) runs only on first start; after a failed first start run `docker compose down -v` and retry.
- Rebuild one service after a code change: `docker compose up -d --build customer-service`.

### Option B: services on your machine, only the database in Docker
```bash
docker compose up -d postgres

./start-all.sh            # builds, starts all 7 services, logs in ./logs   (stop: ./stop-all.sh)
# ... or manually, one terminal each, in this order:
mvn -q clean package -DskipTests
java -jar discovery-server/target/discovery-server-1.0.0.jar     # wait until http://localhost:8761 is up
java -jar api-gateway/target/api-gateway-1.0.0.jar
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar customer-service/target/customer-service-1.0.0.jar
java -jar ledger-service/target/ledger-service-1.0.0.jar
java -jar notification-service/target/notification-service-1.0.0.jar
java -jar payment-service/target/payment-service-1.0.0.jar

cd frontend && npm install && npm run dev        # http://localhost:5173
```
Check http://localhost:8761: the gateway and all five business services should be registered (give them ~30 s).

**Demo logins:** admin → `admin@bfsi.com` / `Admin@123` (seeded). Customers self-register and receive a savings account with a demo balance of ₹50,000.

## Demo flow
1. Register a customer → land on the dashboard (KYC status `NOT_SUBMITTED`).
2. Submit KYC (PAN like `ABCDE1234F`, 12-digit Aadhaar) → status `PENDING`.
3. Log in as admin → *Customers* tab → **Verify** the KYC.
4. Back as the customer → **Pay** (UPI / NEFT / Card). Payments are rejected until KYC is `VERIFIED`.
5. **History** shows payments and the ledger statement; admin sees stats, all transactions, accounts and the audit trail.
6. To see a credit leg: register a second customer, then NEFT/UPI the first customer's account number to them (IFSC `BFSI0000001`).

## API summary (all via `http://localhost:8080/api`)
| Method & path | Who | Purpose |
|---|---|---|
| `POST /auth/register`, `POST /auth/login` | public | Onboarding, JWT |
| `GET /customers/me`, `PUT /customers/me/kyc`, `GET /customers/me/accounts` | customer | Profile, KYC, accounts |
| `GET /customers`, `GET /customers/accounts`, `PUT /customers/{id}/kyc-status?status=VERIFIED\|REJECTED` | admin | Monitoring + KYC decision |
| `POST /payments` (`Idempotency-Key` header optional), `GET /payments/me`, `GET /payments/{txnId}` | customer | Pay / history |
| `GET /payments`, `GET /payments/stats` | admin | Monitoring |
| `GET /ledger/me` / `GET /ledger[?txnId=]`, `GET /ledger/audit` | customer / admin | Statement, ledger, audit trail |
| `GET /notifications/me` / `GET /notifications` | customer / admin | Alerts |

Payment body: `{ "fromAccount", "toAccount", "amount", "mode": "UPI|NEFT|CARD", "ifsc"?, "cardLast4"?, "remarks"? }`
- **UPI**: `toAccount` = VPA (`name@bank`) or account number · limit ₹1,00,000
- **NEFT**: `toAccount` = 9-18 digit account, `ifsc` required · limit ₹10,00,000
- **CARD**: `toAccount` = merchant name, `cardLast4` required · limit ₹2,00,000

Transaction IDs look like `TXN-UPI-20261103101530-K7M2QX`. A payment returns HTTP 200 with `status` = `SUCCESS`, `FAILED` (with `failureReason`, e.g. insufficient funds / KYC) or `PENDING` (outcome unknown, needs reconciliation). Input validation errors return 400.

## Postman
Import `postman/BFSI-Payment-System.postman_collection.json`. It stores `token`, `txnId` and `fromAccount` automatically. Run: Register → Login → Submit KYC → *Login admin* → Verify KYC → *Login customer* → My accounts → Pay.

## Database
`db/schema.sql` documents every table (the services auto-create them via `ddl-auto=update`; switch to `validate` + this script for controlled environments). `db/init.sql` creates the five databases (one per service, no cross-service joins).

## Configuration (env vars)
`DB_HOST`, `DB_USER`, `DB_PASSWORD`, `EUREKA_URL`, `JWT_SECRET` (gateway + auth must match, ≥ 32 chars), `INTERNAL_SECRET` (shared by all backend services), frontend `VITE_API_URL`.

## Design notes
- **Auth at the edge:** the gateway verifies the JWT, strips any client-supplied `X-User-*` headers, and forwards `X-User-Id/Role/Email`. Downstream services trust those headers, so they must only be reachable via the gateway (in production: private network / mTLS / NetworkPolicy).
- **Internal APIs:** `/internal/**` endpoints are not routed by the gateway and also require `X-Internal-Secret`.
- **Money safety:** the payment is persisted as `PENDING` *before* money moves; transfers lock account rows in a fixed order (no deadlocks) inside one DB transaction; `Idempotency-Key` makes client retries safe; ledger/notification failures never undo a completed payment (they are best effort and logged).
- **PII:** Aadhaar is reduced to the last 4 digits; PAN and Aadhaar are masked in API responses.
- **Registration** is compensating: if customer-service is down, the new login is rolled back.

## Known limitations / next steps for production
This is a reference implementation. Before real use add: PAN encryption at rest, refresh tokens + rate limiting / account lockout, a transactional outbox (or Kafka) for ledger/notification delivery instead of synchronous Feign calls, a reconciliation job for `PENDING` payments, circuit breakers (Resilience4j), double-entry ledger with a bank settlement account, distributed tracing, and automated tests. Also, the code was authored without a compile step in the generation environment, so run `mvn clean package` first and report any dependency-version issues.
