# Module Map

## Backend (`backend/src/main/java/com/agripulse/`)

| Module | Contents (foundation) | Later phases |
|---|---|---|
| `common` | BaseEntity, ApiError, GlobalExceptionHandler, HealthController | shared specs |
| `auth` | JwtService, AuthService, SecurityConfig, JwtAuthFilter, AuthController, DTOs | refresh tokens, rate limiting |
| `user` | User, Role, VerificationStatus, UserRepository | profile management |
| `farmer` | FarmerProfile, Farm, repositories | onboarding |
| `buyer` | BuyerProfile, repository | onboarding |
| `fpo` | FpoProfile, repository | member validation flow |
| `crop` | Crop, repository (Tomato seeded) | — |
| `supply` | SupplyReport, SupplyEvidence, service, controller, DTOs | aggregation |
| `demand` | BuyerRequirement, DemandEstimate, DataSource, repositories | estimation job |
| `market` | Market, repository | absorption history |
| `storage` | StorageFacility, repository | booking |
| `transport` | TransportResource, TransportStatus, repository | booking |
| `trust` | VerificationRecord, TrustScore, TrustScoreService | cross-source checks |
| `pressure` | PressureBand, PressureService | snapshots, alerts |
| `simulation` | Scenario, repository | what-if engine |
| `recommendation` | documented placeholder | advisory engine |
| `notification` | Notification, NotificationService | delivery |
| `audit` | AuditLog, AuditService | admin viewer |

## Frontend (`frontend/src/`)

| Folder | Purpose |
|---|---|
| `app/` | router |
| `components/` | StatusBadge, Placeholder |
| `layouts/` | FarmerLayout (bottom tabs), StakeholderLayout (top nav) |
| `pages/` | farmer tabs, buyer/fpo/resources/admin, login, 404 |
| `features/supply/` | report zod schema |
| `services/` | typed fetch client |
| `hooks/` | useAuth |
| `types/` | shared API contracts |
| `utils/` | formatting helpers |
| `styles/` | Tailwind entry |
