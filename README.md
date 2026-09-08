# AgriPulse

Privacy-preserving agricultural network intelligence platform (24-hour hackathon MVP).

Farmers, buyers, FPOs and operators submit supply/demand signals. The system
scores report confidence, estimates regional supply/demand **ranges** (never exact
truth), computes market pressure, and explains the result. Exact personal data and
farm coordinates are never exposed publicly.

## Stack

Pinned versions: see `docs/TECHNOLOGY_VERSIONS.md`.
Java 21 · Spring Boot 3.4.5 · PostgreSQL 17 + PostGIS 3.4 · Flyway ·
React 18 + Vite 6 + Tailwind 3.4 · TypeScript 5.6.

## Prerequisites

- JDK 21+ (runs on 25 LTS)
- Node 22 LTS, npm 10+
- Docker + Docker Compose (for PostgreSQL)

## Setup

```bash
# 1. Database
docker compose up -d

# 2. Backend (from repo root)
cp .env.example .env            # edit secrets, never commit .env
./backend/mvnw -f backend/pom.xml spring-boot:run
# API: http://localhost:8080/api/public/health

# 3. Frontend
cd frontend
cp .env.example .env
npm install
npm run dev                     # http://localhost:5173
```

## Validation

```bash
./backend/mvnw -f backend/pom.xml test     # backend tests (H2, no DB needed)
cd frontend && npm install && npm run build
```

## Project layout

```
backend/src/main/java/com/agripulse/  modular monolith (auth, user, farmer, ...)
backend/src/main/resources/db/migration/  Flyway versioned migrations
frontend/src/  app/ components/ layouts/ pages/ features/ services/ hooks/ types/ utils/ styles/
docs/  TECHNOLOGY_VERSIONS.md, ARCHITECTURE.md, MODULE_MAP.md
```

## Rules for contributors

- Controllers are thin; business logic lives in services; repositories handle persistence.
- Deterministic calculations are plain Java (trust, pressure, simulation) — no AI.
- Every demand/supply figure in the UI carries a REAL / SIMULATED / ESTIMATED label.
- Never expose exact farm coordinates, bank details or personal data in public APIs.
- Never commit secrets. Copy `.env.example`, never `.env`.
