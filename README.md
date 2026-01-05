# Lobo Soft Monorepo

A small banking stack with multiple Spring Boot services, a Python categorization service, and an iOS client. Local dev runs via docker-compose with Postgres.

## Services
- `auth` – AuthN/AuthZ with JWT + gRPC.
- `enabel-banking-service` – EnableBanking integration + gRPC.
- `api` – Public HTTP API gateway.
- `sync` – Background sync service (Kafka-enabled).
- `categorization` – Python service for transaction categorization.
- `BankApp` – iOS client (SwiftUI).

## Prerequisites
- Docker + Docker Compose
- JDK 17 (for local service builds), Maven
- Python 3.11+ (for categorization service)
- Xcode (for BankApp)

## Quickstart (docker-compose)
1) Copy env template and fill values:
```
cp .env.example .env
# edit .env with your secrets (DB password, JWT secret, etc.)
```
2) Start services:
```
docker compose up --build
```
3) API endpoints will default to `http://localhost:8085`. Auth service on `8082`, enable-banking on `8081`, sync on `8080`, Postgres on `5433`.

### Environment variables
All runtime configuration comes from env vars; see `.env.example` for the full list. Do not commit `.env` or real secrets (JWT secret, DB password, private keys). The EnableBanking private key should be mounted at runtime (e.g., Docker secret/volume) and never committed.

## Local development per service
- **auth / api / enabel-banking-service / sync**: Spring Boot; use `mvn spring-boot:run` in each folder with env vars set (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, etc.).
- **categorization**: Create a virtualenv, install deps (`pip install -r requirements.txt` if present), and run the service entrypoint (see folder docs if present).
- **iOS BankApp**: Update the API base URL in build settings or a config file (currently hardcoded in `BankApp/BankApp/Networking/APIClient.swift`). Open `BankApp/BankApp.xcodeproj` in Xcode, set signing, and run on a simulator/device.

## Repository layout
```
.
├─ api/                    # Public API gateway (Spring)
├─ auth/                   # Auth/JWT + gRPC
├─ enabel-banking-service/ # EnableBanking integration
├─ sync/                   # Background sync service
├─ categorization/         # Python categorization service
├─ BankApp/                # iOS client (SwiftUI)
├─ db/                     # Postgres init SQL
├─ docker-compose.yml      # Local dev stack
├─ .env.example            # Env template (copy to .env)
└─ .gitignore
```

## Security and secrets
- Do not commit `.env`, private keys (e.g., `certs/app.pem`), or real passwords.
- Use GitHub Actions secrets or deployment secret stores for production.

## Common tasks
- Build images: `docker compose build`
- Start stack: `docker compose up`
- Stop stack: `docker compose down`
