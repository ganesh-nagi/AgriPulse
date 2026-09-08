# Technology Versions

| Technology | Version | Stable/Supported | Source | Reason |
|---|---|---|---|---|
| Java | 21 | Stable LTS | https://www.oracle.com/java/ | Backend runtime (pom release 21, runs on JDK 25 LTS) |
| Spring Boot | 3.4.5 | Stable | https://spring.io/projects/spring-boot | BOM-managed framework (last 3.4.x stable, Java 21 compatible) |
| Spring Security | Managed by Boot BOM | Stable | https://spring.io/projects/spring-security | Managed by Spring Boot BOM |
| Hibernate | Managed by Boot BOM (6.6.x) | Stable | https://hibernate.org/ | Managed by Boot BOM |
| Maven | 3.9.9 (wrapper) | Stable | https://maven.apache.org/ | Build tool |
| PostgreSQL | 17.11 (image postgres:17-alpine) | Supported | https://www.postgresql.org/support/versioning/ | Database, supported until 2029 |
| PostGIS | 3.4 (image postgis/postgis:17-3.4-alpine) | Stable | https://postgis.net/ | Compatible with PG 17 |
| Flyway | Managed by Boot BOM (10.x) | Stable | https://documentation.red-gate.com/fd | Migrations |
| React | 18.3.1 | Stable | https://react.dev/ | Frontend UI |
| React DOM | 18.3.1 | Stable | https://react.dev/ | Frontend |
| Vite | 6.4.3 | Stable (patched) | https://vite.dev/ | Build tool - upgraded from 6.2.5 for GHSA security fix |
| TypeScript | 5.6.3 | Stable | https://www.typescriptlang.org/ | Frontend types |
| Tailwind CSS | 3.4.17 | Stable | https://tailwindcss.com/ | Styling |
| React Router | 6.28.0 | Stable | https://reactrouter.com/ | Routing |
| React Hook Form | 7.54.2 | Stable | https://react-hook-form.com/ | Forms |
| Zod | 3.24.2 | Stable | https://zod.dev/ | Validation |
| Recharts | 3.1.0 | Stable | https://recharts.org/ | Charts - upgraded from 2.13.3 (deprecated) |
| Leaflet | 1.9.4 | Stable | https://leafletjs.com/ | Maps |
| Node.js | 22 LTS (requires >=18) | Stable LTS | https://nodejs.org/ | Frontend runtime |
| JUnit | 5 (managed by Boot) | Stable | https://junit.org/ | Testing |
| Docker | 4.x stable | Stable | https://www.docker.com/ | Infrastructure |

> Note: Spring Boot 4.1.1 is latest stable but requires Spring Framework 7 ecosystem migration. For MVP stability with inexperienced team, Boot 3.4.x (Java 21) is the newest well-supported compatible release per policy ("if newest creates compatibility risk, prefer well-supported compatible release"). Local JDK 25 runs Java 21 bytecode.

Pinning: Docker images pinned to minor (17-alpine, 17-3.4-alpine). Frontend lockfile committed. Maven Wrapper committed.
