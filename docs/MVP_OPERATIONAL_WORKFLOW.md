# AgriPulse MVP — Operational Workflow Guide

Factual end-to-end guide generated from the current repository only.
No behavior described here goes beyond what the code implements.
Counts: **19 controllers, 42 endpoints, 14 frontend routes, 6 roles,
16 test classes / 100 tests.**

Stack: Spring Boot 3.4.5 (Java 21) → PostgreSQL 17 / PostGIS 17-3.4,
Flyway V1–V6, React 18 + React Router 6 + Leaflet 1.9.4 + Tailwind 3.4.

---

## 1. Startup procedure

```powershell
# 1. Database (from repo root; volume pgdata persists state)
docker compose up -d
docker compose ps   # db must show "Up", 0.0.0.0:5432->5432/tcp

# 2. Backend (from backend/)
.\mvnw.cmd spring-boot:run
# Expect: Hikari connected → Flyway "Successfully applied 6 migrations ...
# now at version v6" → Hibernate initialized → "Tomcat started on port 8080"
# → "Started AgriPulseApplication"

# 3. Frontend (from frontend/)
npm install
npm run dev     # Vite dev server
npm run build   # tsc && vite build → dist/
```

Demo profile (synthetic data only, never production):
`.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo`,
then `POST /api/demo/reset`.

## 2. Required environment variables

Backend (`backend/src/main/resources/application.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/agripulse` | JDBC URL |
| `DATABASE_USERNAME` | `agripulse` | DB user |
| `DATABASE_PASSWORD` | `agripulse` | DB password |
| `JWT_SECRET` | dev-only placeholder | HS256 key, **must be ≥ 32 bytes** or app fails fast at startup |
| `JWT_EXPIRATION_MS` | `86400000` | Access-token lifetime (24 h) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh-token lifetime (7 d) |

Fixed tunables (not env): lockout after **5** failed attempts,
lock window **15 min** (`app.auth.*`), rate limit **20 req / 60 s**
on auth/verification/FPO-validation endpoints (`app.rate-limit.*`).

Frontend (`frontend/.env.example` → copy to `.env`): `VITE_API_URL=http://localhost:8080`.
Only `.env.example` files are committed; real `.env` files are git-ignored.

## 3. Database and Flyway setup

- `docker-compose.yml`: image `postgis/postgis:17-3.4-alpine`,
  `POSTGRES_DB/USER/PASSWORD = agripulse`, port `5432:5432`, volume `pgdata`.
- `spring.jpa.hibernate.ddl-auto: validate`, `flyway.enabled: true`.
- Migrations `V1__baseline.sql` … `V6__geo_coordinates.sql` create all tables
  (users, crops, farms, farmer_profiles, buyer_profiles, fpo_profiles,
  supply_reports, supply_evidence, trust_scores, buyer_requirements,
  demand_estimates, markets, market_observations, storage_facilities,
  transport_resources, processing_facilities, refresh_tokens, audit_log,
  notifications, verification_records, scenarios, …), seed reference rows
  (Tomato crop, Nashik reference market/storage/processing), and add
  trust-assessment, observation, processing/travel, and geo columns.
- Tests use H2 (`create-drop`, Flyway off); production/dev use Postgres + Flyway.

## 4–8. Complete endpoint catalog, auth, roles, examples, errors

Auth model: stateless JWT (`Authorization: Bearer <token>`).
Public (no token): `/api/auth/register`, `/login`, `/refresh`,
`/api/public/**`. `POST /api/auth/logout` requires authentication.
`GET /api/crops` requires authentication (any role). Demo endpoints are
`permitAll` but only exist on the `demo` profile. Everything else is
role-gated via `@PreAuthorize`; services additionally scope data to the
JWT owner. Guards in React (`RequireAuth`, `RequireRole`) are UX-only.

Error contract (`ApiError{timestamp,status,message,fieldErrors?}`):
`400` validation (with `fieldErrors`) or business-rule message;
`401` expired/invalid token (frontend clears session → `/login`);
`403` `"Access denied"`; `423` locked account; `429` rate limit;
`500` `"Unexpected error"` (never stack traces).

### Auth — `AuthController /api/auth` (logout: authenticated; rest public)

- `POST /api/auth/register` → `{"email","password","role"}` →
  `201` + AuthResponse. Validation: email format, password policy.
- `POST /api/auth/login` → `{"email","password"}` → `200` + AuthResponse.
  5th consecutive failure locks the account (isolated transaction, `423` after).
- `POST /api/auth/refresh` → `{"refreshToken"}` → `200` + AuthResponse.
  Old token revoked on use (rotation); revoked/expired rejected.
- `POST /api/auth/logout` (Bearer required) → `204`. Revokes all user
  refresh tokens + denylists access-token `jti` until its real expiry.

`AuthResponse{token, refreshToken, email, role}` — frontend stores them as
`agripulse.token/.refresh/.email/.role`.

### Crops / health / regional (reference + public)

- `GET /api/crops` (auth) → `[{id,name,unit}]` (e.g. Tomato/tonne).
- `GET /api/public/health` (public) → liveness.
- `GET /api/public/regional/supply?region=` (public) →
  `RegionalSupplyView{region,cropName,reportCount,quantityMinTotal,quantityMaxTotal,averageConfidence,dataSource=ESTIMATED}`.
  Aggregates only — no identities, coordinates, or individual quantities.

### Farmer — FARMER role

Supply reports (`SupplyReportController /api/reports`):
- `POST /api/reports` → `{farmId,cropId,quantityMinTonnes,quantityMaxTonnes,harvestStart,harvestEnd,quality?,region,latitude?,longitude?}`
  (rules: min ≤ max, max > 0, ordered dates, owned farm, existing crop,
  ACTIVE account) → `201` + `SupplyReportResponse{id,farmId,cropName,quantityMin/Max,harvestStart/End,quality,region,status,trustScore,trustLevel}`.
  Status is always `SUBMITTED`; trust is scored immediately.
- `GET /api/reports/mine` → own reports.
- `GET /api/reports/{id}` → one own report (others' → `403`).
- `PATCH /api/reports/{id}` → `{quantityMin/Max,harvestStart/End,quality}`;
  only DRAFT/SUBMITTED; re-scored.
- `POST /api/reports/{id}/cancel` → `CANCELLED` (DRAFT/SUBMITTED only).

Farms (`FarmController /api/farmer/farms`):
- `POST /api/farmer/farms` → `{name,region,latitude?,longitude?,areaAcres?}` → `201`.
- `GET /api/farmer/farms/mine` → `[{id,name,region,areaAcres}]`.

Verification (`VerificationController /api/verification`):
- `POST /profile` → `{fullName,phone?,region}` → `201` + status.
- `GET /status` → `OnboardingStatusResponse{state,phoneVerified,identityStatus,fpoValidated,regionConsistent,evidenceCount}`.
- `POST /phone/request` → `{state,demoCode,note}` (mock provider);
  `POST /phone/confirm` → `{code}`.
- `POST /identity/request`, `POST /identity/confirm` → mock KYC steps.
- `POST /farm/{farmId}` → farm-region consistency check.

Intelligence (read-only, regional aggregates):
- `GET /api/pressure?cropId&region&from&to` →
  `PressureAssessment{band,effectiveSupplyTonnes,estimatedMin/MaxTonnes,reasons[{component,message}]}`.
- `GET /api/demand/estimate?...` (same params) →
  `DemandEstimateResponse{confirmedDemandTonnes,confirmedBuyerCount,absorptionMin/Max,estimatedMin/Max,confidence,dataSource,provenance[]}`.
  Calling it persists one `DemandEstimate` row.
- `GET /api/resources/snapshot?region&cropId&date` →
  `ResourceSnapshot{storage/transport/processingAvailableTonnes,marketAbsorptionMin/Max}`.
- `GET /api/markets?region` → `MarketNodeView{id,name,region,absorptionMin/Max,marketType,latitude,longitude}` (public nodes only).
- `GET /api/geo/nearby?lat&lon&radiusKm(0,2000]&kinds&crop` →
  `GeoNode{kind,id,name,region,latitude,longitude,distanceKm,summary,availableTonnes}` (Haversine; markets/storage/processing only).

### Buyer — BUYER role (`BuyerRequirementController /api/buyer/requirements`)

- `POST /api/buyer/requirements` →
  `{cropId,quantityTonnes(≥0),quality?,requiredDate,region}` → `201` +
  `{id,cropName,quantityTonnes,quality,requiredDate,region,status=OPEN,dataSource=REAL}`.
- `GET /api/buyer/requirements/mine` → own requirements with status.

### FPO — FPO role (`FpoController /api/fpo`)

- `GET /api/fpo/members?region` →
  `FpoMemberView{farmerUserId,farmerName,region,verificationStatus,onboardingState,fpoValidated,reportCount}`.
  No phones, coordinates, or individual quantities.
- `POST /api/fpo/validations` → `{farmerUserId,notes?}` → `201` (rate-limited).

### Storage operator — STORAGE_OPERATOR (`StorageFacilityController /api/storage`)

- `POST /api/storage` →
  `{name,region,capacityTonnes(≥0),cropCompatibility?,costPerDay?,availableFrom?,availableTo?}` → `201` +
  `{id,name,region,capacityTonnes,occupiedTonnes,availableTonnes}`.
- `GET /api/storage/mine` → own facilities.
- `PATCH /api/storage/{id}/occupancy` → `{occupiedTonnes}`; rejected if
  negative or above capacity. Zero capacity/occupancy is valid.

### Transporter — TRANSPORTER (`TransportResourceController /api/transport`)

- `POST /api/transport` →
  `{capacityTonnes(≥0),availableFrom?,availableTo?,originRegion,destRegion?,costPerKm?,travelEstimateDays?}` → `201` +
  `{id,capacityTonnes,originRegion,destRegion,status=AVAILABLE}`.
- `GET /api/transport/mine` → own resources.
- `PATCH /api/transport/{id}/status` → `{status: AVAILABLE|BOOKED|IN_TRANSIT|MAINTENANCE}`.

### Admin — ADMIN role

- `GET /api/admin/overview` →
  `AdminOverviewResponse{totalUsers,usersByRole,reportsByStatus,averageTrustScore,openRequirements,storageFacilities,transportResources}` (counts only).
- `PUT /api/admin/users/{id}/role`, `PUT /api/admin/users/{id}/status` (audit-logged).
- `POST /api/scenarios/simulate` (`ScenarioController`) →
  `{label, input:{cropId,region,windowStart,windowEnd,supplyDeltaTonnes,demandDeltaTonnes,storage/transport/processingOverrideTonnes?}}` →
  `ScenarioResult{label,baseline:PressureAssessment,scenario:PressureAssessment}`.
  Read-only; overrides must be ≥ 0, deltas finite.

### Demo — demo profile only, open (`DemoController /api/demo`)

- `POST /api/demo/reset` → rebuilds deterministic synthetic data, returns summary.
- `POST /api/demo/scenarios/{healthy|high-supply|suspicious|no-storage|transport-shortage|strong-demand}` → applies on a fresh base, returns summary (band, effective vs estimated range, notes).
- `GET /api/demo/status` → scenario list + demo credentials.

---

## 9. Role-based UI workflows

Conventions: `RequireAuth` redirects token-less users to `/login`;
`RequireRole` redirects wrong roles to `/`; expired sessions are cleared on
`401`. All pages show loading/error/empty states.

### FARMER (`/`, `/markets`, `/report`, `/alerts`, `/profile`, `/onboarding`, `/verify`)

- **Register** — USER ACTION: open `/register`, enter email/password/role=FARMER
  → API: `POST /api/auth/register` → SERVICE: `AuthService.register` (BCrypt hash)
  → DB WRITE: `users` → RESULT: tokens stored, role=FARMER → NEXT: onboarding.
- **Login** — `/login` → `POST /api/auth/login` → `AuthService.login`
  (+`LoginAttemptService` lockout counting) → DB READ `users` (+WRITE attempt/audit)
  → RESULT: session → NEXT: home (or `/verify` if unverified).
- **Verification** — `/verify` (+`/onboarding` profile/farm setup) →
  `POST /api/verification/profile`, `/phone/request|confirm`, `/identity/request|confirm`, `/farm/{id}`, `GET /status` →
  `FarmerOnboardingService` (mock providers) → DB WRITE `farmer_profiles/farms/verification_records`
  → RESULT: state advances to `PROFILE_ACTIVE`, verification tier rises → NEXT: higher trust weight.
- **Farm/profile** — `/onboarding` → `POST /api/farmer/farms`, `GET /mine` → `FarmService` → DB `farms` → RESULT: owned farm list (region source for all queries).
- **Supply report** — `/report` 4-step form (Crop → Quantity → Dates & quality → Review) →
  `POST /api/reports` → `SupplyReportService.create` (ownership/crop/range/date/ACTIVE checks)
  → DB WRITE `supply_reports` + `trust_scores` + `audit_log` → RESULT: response shows status + confidence → NEXT: report appears under “My reports”.
- **View reports** — `/report` list → `GET /api/reports/mine` (+`GET /{id}`, `PATCH /{id}`, `POST /{id}/cancel`)
  → DB READ/WRITE own rows only → RESULT: edit/cancel with re-scoring.
- **Markets** — `/markets` → `GET /api/markets?region` + pressure/demand/snapshot + `GET /api/geo/nearby` on Leaflet/OSM map →
  `MarketController/PressureService/DemandEstimationService/ResourceStateService/GeoService` → DB READ public nodes/aggregates
  → RESULT: market cards + map markers (icon+label, click popup) → NEXT: decide where to sell.
- **Alerts** — `/alerts` → pressure reasons + verification status + own `REQUIRES_REVIEW` reports
  → RESULT: actionable items only (pressure constraints, verify, review report).
- **Profile** — `/profile` → `GET /api/verification/status`, `GET /api/reports/mine` (count), language, privacy note → RESULT: account overview.
- **Logout** — Profile logout → `POST /api/auth/logout` → refresh tokens revoked + access `jti` denylisted → DB WRITE → RESULT: session cleared → NEXT: `/login`.

### BUYER (`/buyer`)

- **Register/Login** — as farmer, role=BUYER → NEXT: workspace.
- **Demand submission** — requirement form → `POST /api/buyer/requirements` → `BuyerRequirementService.create` (BUYER+ACTIVE, auto-creates profile)
  → DB WRITE `buyer_requirements` (status OPEN, source REAL) + audit → RESULT: appears in “My requirements”.
- **Supply/market view** — workspace → `GET /api/public/regional/supply?region` + `GET /api/pressure?...` + demand estimate
  → aggregates only, zero farmer identity → RESULT: decide volumes/timing.
- **Logout** — as farmer.

### FPO (`/fpo`)

- **Login** — role=FPO.
- **Member/aggregated supply** — region loader → `GET /api/fpo/members?region` + regional supply aggregates
  → `FpoService` → DB READ profiles/reports (validation state only) → RESULT: member table (verification, report counts, validated flag).
- **Validate** — Validate action → `POST /api/fpo/validations` `{farmerUserId,notes?}` → DB WRITE validation + audit
  → RESULT: member `fpoValidated=true`, future trust +25 → NEXT: rescoring flows through.
- **Demand/resources** — demand estimate + resource snapshot → RESULT: regional picture.
- **Logout** — as farmer.

### STORAGE OPERATOR (`/resources`, role-gated section)

- **Login** — role=STORAGE_OPERATOR.
- **Capacity** — facility form → `POST /api/storage` → `StorageFacilityService.create` (capacity ≥ 0, ordered window)
  → DB WRITE `storage_facilities` → RESULT: listed with totals.
- **Availability** — occupancy edit → `PATCH /api/storage/{id}/occupancy` (0 ≤ occupied ≤ capacity)
  → DB WRITE → RESULT: available = capacity − occupied (never negative).
- **Resources** — `GET /api/storage/mine` → RESULT: totals + per-facility bars + compatibility.
- **Logout** — as farmer.

### TRANSPORTER (`/resources`, role-gated section)

- **Login** — role=TRANSPORTER.
- **Capacity** — resource form → `POST /api/transport` → `TransportResourceService.create`
  → DB WRITE `transport_resources` (AVAILABLE) → RESULT: fleet list.
- **Availability** — status edit → `PATCH /api/transport/{id}/status` → DB WRITE → RESULT: only AVAILABLE counts toward demand.
- **Regional demand** — guidance panel (keep fleet ready when pressure HIGH/CRITICAL and storage limited).
- **Logout** — as farmer.

### ADMIN (`/admin`)

- **Login** — role=ADMIN.
- **Regional dashboard** — `GET /api/admin/overview` → counts/averages only → RESULT: platform cards.
- **Supply/demand/pressure/resources drill-down** — region+crop+window loader → regional supply + `GET /api/demand/estimate` + `GET /api/pressure` + `GET /api/resources/snapshot`
  → RESULT: 2×2 intelligence grid with bands, ranges, reasons.
- **Trust/anomalies** — average trust + flagged SUPPLY reasons + status breakdown → RESULT: review narrative (no fraud labels exist).
- **Map** — admin uses the farmer `/markets` map semantics (public nodes only).
- **Scenario simulation** — deltas/overrides form → `POST /api/scenarios/simulate` → `ScenarioSimulationService` (read-only)
  → DB: no writes → RESULT: baseline vs scenario bands + reasons.
- **Allocation** — engine is backend-only in MVP (no UI); reachable in a full build via `AllocationService.allocate` contract
  (greedy: storage → processing → buyer demand → alternative markets, honest remaining exposure).
- **Logout** — as farmer.

---

## 10–17. Internal business flow (verified logic, no AI/ML anywhere)

**Supply report → Trust.** `SupplyReportService.create` validates, saves
(`SUBMITTED`, never auto-rejected), then `persistAssessment` builds
`TrustSignals` (verification tier, evidence attached, FPO flag, farm-vs-profile
region match, past-report history excl. current, peer-median quantity deviation,
temporal sanity, cross-source quality match) and stores
`TrustScore{score,level,requiresReview,signals}`. Update/cancel re-run or reuse it.

**Trust-score process (fixed weights, deterministic, capped [0,95] — never 100):**
base +20; phone +10; identity +20; FPO +25; evidence +15; region match +10;
history avg ≥70 +10 / 50–70 +5 / new reporter −5; peer-aligned +5;
deviation >2× −8 / >3× −15; temporal ok +5 / bad −10.
Bands: ≥70 HIGH, ≥45 MEDIUM, ≥25 LOW, else REQUIRES_REVIEW; review also on
extreme deviation or bad window. Missing evidence alone never rejects; there is
no fraud label. Same input → same output (pure function, tested).

**Supply estimation (effective supply).**
`PressureService.effectiveSupply` = Σ over SUBMITTED/VALIDATED reports of
`midpoint × confidence/100` per crop+region. Raw reports never directly control
regional supply; low confidence shrinks influence (missing trust row = full
weight fallback, practically unreachable since scoring is atomic with create).

**Suspicious reports.** Large peer deviation (≥3×) forces `requiresReview`
and −15 confidence; the report stays SUBMITTED but contributes little weight
and surfaces in FPO review and farmer Alerts.

**Demand estimation.** `DemandEstimationService`:
CONFIRMED = Σ OPEN requirements in window;
OBSERVED = market observations for crop+region with period end in the prior
24 months, absorption = [min of mins, max of maxes];
ESTIMATED = [confirmed+absMin, confirmed+absMax].
Confidence: base 20, confirmed present +15, ≥2 buyers +10, 1/2–3/4+ obs
+15/+25/+35, all-SIMULATED −10; clamped [5,95]. `estimate()` persists one row;
`computeUnsaved()` is the identical read-only math for the simulator.

**Market-pressure calculation.** `PressureService.evaluate` (pure, shared by
production and scenarios): supply vs estimate range → LOW (shortage) /
MODERATE (in range) / HIGH (above max) / 20%+ over max CRITICAL; storage
coverage ≥1 LOW, ≥0.25 MODERATE, >0 HIGH, zero MODERATE + must-move-fresh note
(absence never escalates alone); transport ≥1 LOW, ≥0.5 MODERATE, >0 HIGH,
zero HIGH; processing is context-only; confirmed <30% of estMax with supply
≥MODERATE escalates one band (cap CRITICAL). Overall = highest band + full
reason list. A condition indicator, never a price prediction.

**Resource handling.** `ResourceStateService.snapshot` sums compatible,
in-window availability (storage compatibility blank = any crop, else
comma-separated names). Storage/processing/transport are optional: zero is
valid everywhere; missing rows contribute 0 without errors.

**Scenario simulation.** `ScenarioSimulationService.simulate` is
`@Transactional(readOnly=true)`: reads production state, shifts effective
supply and the demand range in memory, substitutes resource overrides, and
re-runs the shared `evaluate()`. `Scenario` is deliberately not a JPA entity.
Invalid windows/negative overrides/non-finite deltas are rejected. Repeated
identical input → identical output (tested, incl. reason text).

**Resource allocation.** `AllocationService.allocate` (read-only, deterministic
greedy, explicitly not an AI optimizer; OR-Tools intentionally omitted):
compatible storage (cheapest first) → processing → confirmed buyer demand →
alternative markets capped by the shared AVAILABLE-transport pool; windows,
compatibility and capacities are hard constraints; costs use known rates only
(labelled estimates, null otherwise); `AllocationResult` reports per-target
entries plus `remainingExposureTonnes` honestly when capacity is insufficient.

## 18. Data labels

- **REAL** — platform-entered buyer requirements, observed market imports.
- **SIMULATED** — seeded/imported demo rows (explicitly labelled; seed discount −10 in demand confidence).
- **ESTIMATED** — every computed range, score, band, and confidence (capped 95/never 100).
Labels travel in DTOs (`dataSource`, `provenance[]`); the UI renders them beside every figure.

## 19. Privacy visibility matrix

| Data | Farmer | Buyer | FPO | Operator/Transporter | Admin | Public |
|---|---|---|---|---|---|---|
| Own reports/requirements/fleet | own rows | own rows | — | own rows | — | — |
| Other farmers' identity/qty/coords | never | never | never | never | never | never |
| Regional aggregates (ranges, counts, avg confidence) | yes | yes | yes | — | yes | supply endpoint |
| Buyer identities | never | own only | never | never | counts only | never |
| FPO member validation state | — | — | permitted view | — | — | never |
| Public market/storage/processing nodes | yes (map) | — | — | — | — | markets are public infra |

Exact farm coordinates stay server-side; `SupplyReportResponse`/`FarmResponse`
carry no coordinates; `GeoNode` exposes public infrastructure only.

## 20. Final project counts

Controllers **19** · endpoints **42** · frontend routes **14** · roles **6**
(FARMER, BUYER, FPO, STORAGE_OPERATOR, TRANSPORTER, ADMIN) ·
test classes **16** · tests **100/100 passing** · migrations **V1–V6** ·
backend jar builds · frontend `tsc && vite build` clean.

## 21. Exact local-execution commands

```powershell
docker compose up -d                  # PostgreSQL/PostGIS :5432 (volume pgdata)
cd backend
.\mvnw.cmd clean test                 # 100 tests (H2, Flyway off)
.\mvnw.cmd spring-boot:run            # API on http://localhost:8080 (Flyway migrates)
cd ..\frontend
npm install
# ensure .env contains: VITE_API_URL=http://localhost:8080
npm run dev                           # Vite dev server
npm run build                         # production bundle → dist/
```
Demo mode: backend with `-Dspring-boot.run.profiles=demo`, then
`POST /api/demo/reset`, `POST /api/demo/scenarios/{healthy|high-supply|suspicious|no-storage|transport-shortage|strong-demand}`,
`GET /api/demo/status` (demo logins + password).
