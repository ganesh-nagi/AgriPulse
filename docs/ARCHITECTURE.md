# AgriPulse Architecture (Foundation)

Modular monolith. One deployable Spring Boot API, one React SPA, one PostgreSQL/PostGIS database.

```
React + TypeScript + Vite (frontend/)
        |  REST/JSON, JWT Bearer
Spring Boot API (backend/) — thin controllers, services hold rules
        |  JPA/Hibernate, Flyway migrations
PostgreSQL 17 + PostGIS (docker-compose.yml service `db`)
```

## Backend layers (per module)

`controller` (REST, validation, auth) → `service` (business rules) →
`repository` (persistence) → `entity` (tables). API contracts use DTOs;
entities are never returned directly where privacy matters.

## Security

- BCrypt password hashes only; ADMIN cannot self-register.
- Stateless JWT (jjwt HS256); `JwtAuthFilter` sets the security context.
- Public: `/api/auth/**`, `/api/public/**`. Everything else authenticated;
  role checks via `@PreAuthorize` (e.g. farmers create reports).
- Central `GlobalExceptionHandler` returns safe error messages, no stack traces.
- Audit log for register/login/report actions (no secrets in logs).

## Data principles enforced by the schema

- Supply quantities are min/max ranges; demand has confirmed/absorption/estimated
  bands plus confidence and a REAL/SIMULATED/ESTIMATED label.
- Coordinates are nullable and server-side only; aggregate views exclude them.
- Trust scores are 0–95 with a signal summary, never "100% verified".
- Storage/transport carry availability windows and regions; absence is normal.

## Frontend

- `app/router.tsx` maps farmer tabs (Home, Markets, Report, Alerts, Profile)
  and stakeholder areas (buyer, fpo, resources, admin) to placeholder pages.
- `services/api.ts` is the only HTTP layer; `hooks/useAuth.ts` holds the JWT.
- `features/supply/reportSchema.ts` mirrors server validation client-side
  (the server always re-validates).
- One primary accent (`primary` green) + neutrals; status shown as icon + label.
