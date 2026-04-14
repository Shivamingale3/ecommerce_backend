# Invoice Platform — Spring Boot Project Analysis

## Current State

A bare Spring Boot 4.0.5 skeleton with only `spring-boot-starter-webmvc`, `devtools`, `docker-compose`, `postgresql`, and `lombok`. No security, no validation, no API docs, no testing beyond a context load smoke test.

---

## What's Missing

### Security

| Gap | Risk |
|-----|------|
| **No Spring Security** — zero authentication or authorization | Any endpoint is publicly accessible. Critical for a financial platform. |
| **No CSRF protection** | Vulnerable to cross-site request forgery on state-changing calls. |
| **No input validation** — no `@Valid`, no Bean Validation | No structural defense against malformed/malicious payloads. |
| **No password encoding** | Credentials stored/checked in plain or trivial schemes. |
| **No rate limiting** | APIs are vulnerable to brute-force and DoS. |
| **No account lockout / brute-force protection** | Login endpoints can be hammered indefinitely. |
| **No secrets management** | DB password in `application.properties` or `compose.yaml` in plaintext. |
| **No HTTPS enforcement** | No redirect from HTTP, no HSTS header. |
| **No security headers** | Missing CSP, X-Content-Type-Options, X-Frame-Options, etc. |
| **No audit logging** | No trail of who did what and when — essential for financial compliance. |
| **No API key / token rotation mechanism** | No lifecycle management for credentials. |
| **No domain-level CORS policy** | Cross-origin requests uncontrolled. |
| **No data masking in logs** | PII / financial amounts could leak into logs. |
| **No OpenAPI security scheme documentation** | API consumers have no visibility into auth requirements. |

### Developer Experience

| Gap | Impact |
|-----|--------|
| **No API docs** (SpringDoc/OpenAPI) | Frontend and external consumers have no contract to work against. |
| **No meaningful tests** | Only a context-load smoke test. No service, repository, or controller tests. |
| **No Docker build in pom.xml** | No `spring-boot:build-image` or Jib integration. |
| **No testcontainers** | Tests run against a real Postgres but not isolated per-test. |
| **No seed data / Flyway/Liquibase** | Database schema is manual. No migration history. |
| **No structured logging config** | Just default Spring Boot logging, no JSON log format for tooling. |
| **No request/response logging filter** | No debug trace of API traffic. |
| **No global exception handler** | Errors surface as raw stack traces or unhelpful defaults. |
| **No profile-based config** (`dev`, `staging`, `prod`) | No way to vary config across environments. |
| **No `.env.example`** | Developers don't know what env vars to set. |
| **No custom startup banner** | No project branding. |
| **No Actuator** | No health checks, metrics, or shutdown endpoints. |
| **No build-time env var injection** (like `@jar-bootstrap` or Spring secrets) | Hardcoded fallbacks at risk of being committed. |

### Domain / Business Logic

| Gap | Why It Matters |
|-----|---------------|
| **No domain entities** (Invoice, Client, Payment, Reminder) | Everything is stubs. |
| **No JPA / Hibernate setup** | No ORM for persistence. |
| **No service layer** | All logic lives in controllers. |
| **No repository layer** | No data access abstraction. |
| **No DTOs** | Entities exposed directly — couples API to DB schema. |
| **No enums for invoice status, payment status, currency** | Statuses managed as raw strings — prone to typos and inconsistencies. |
| **No sequence/generator strategy for invoice numbers** | Invoice numbers need a predictable, gap-free, format (e.g. `INV-2026-000001`). |
| **No PDF generation** | Core feature for invoice delivery. |
| **No email/SMTP config** | Payment reminders need email. |
| **No multi-tenancy consideration** | Platform likely needs tenant isolation (schema-per-tenant or row-level). |

---

## Recommendations

### Phase 1 — Foundation (Do First)

#### Security
- Add `spring-boot-starter-security` with JWT-based stateless auth.
- Use `spring-boot-starter-validation` + `jakarta.validation` annotations on all DTOs.
- Add `spring-boot-starter-actuator` with `/health` and `/info` exposed; secure `/actuator` endpoints.
- Configure `BCryptPasswordEncoder` for any stored credentials.
- Add `spring-boot-starter-mail` + SMTP properties for reminders.
- Add a `SecurityHeadersFilter` (or use `HttpSecurity#headers()`) for CSP, X-Frame-Options, HSTS, etc.
- Configure CORS with explicit allowed origins — no wildcard in production.
- Use Spring's `@Scheduled` + `MutexLock` or ShedLock for scheduled reminder jobs.
- Enable SSL in `application-prod.properties`; enforce HTTPS via redirect.
- Add a custom `AuditLog` entity + `ApplicationEventListener` to log all mutating operations.

#### Configuration & DX
- Switch to `application.yml` (not `.properties`) for YAML profile inheritance.
- Create `application-dev.yml`, `application-staging.yml`, `application-prod.yml` with appropriate overrides.
- Add `.env.example` with all required env vars documented.
- Add `spring-boot-starter-data-jpa` with Flyway migrations (not `spring-boot-starter-data-jpa` alone — use Flyway for schema management, not Hibernate `ddl-auto`).
- Add `springdoc-openapi-starter-webmvc-ui` for Swagger UI at `/swagger-ui.html`.
- Add `spring-boot-starter-actuator` for health/metrics endpoints.
- Add Logback JSON encoder for structured logging (needed by ELK/Grafana Loki).
- Add a proper global exception handler with `@ControllerAdvice`, mapping to a consistent error response DTO (status, message, timestamp, path, traceId).

#### Testing
- Add `spring-boot-starter-test` with `spring-security-test` integration.
- Add `testcontainers` for PostgreSQL in integration tests — isolated, repeatable, fast.
- Write controller tests (MockMvc) for all REST endpoints.
- Write service-level unit tests with mocked repositories.
- Add a `BaseIntegrationTest` class that spins up testcontainers once per class or method.
- Add `assertj` instead of vanilla JUnit assertions for readability.

### Phase 2 — Domain & Features

#### Domain
- Model: `Client`, `Invoice`, `InvoiceLineItem`, `Payment`, `Reminder`, `Tenant`, `User`, `AuditLog`.
- Use JPA auditing (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`) — critical for financial audit trails.
- Use enums: `InvoiceStatus` (DRAFT, SENT, OVERDUE, PAID, CANCELLED), `PaymentMethod`, `ReminderType`.
- Implement a `SequenceGenerator` service for invoice numbers with configurable format.
- Add `@Version` on all entities for optimistic locking — prevents concurrent update races.

#### API Design
- RESTful resource-based URLs: `POST /api/v1/invoices`, `GET /api/v1/invoices/{id}`, `PATCH /api/v1/invoices/{id}`, etc.
- Version all APIs under `/api/v1/` — enables safe evolution.
- Use DTOs exclusively — never expose entities directly in API contracts.
- Add pagination to list endpoints (`Pageable`, `Page` response wrapper).
- Add filtering/sorting on list endpoints (e.g., by status, date range, client).
- Add HATEOAS links to list responses (optional but nice for API discoverability).

#### PDF & Notifications
- Add `openhtmltopdf` or `itext7` for server-side PDF invoice generation.
- Template invoices with Thymeleaf → PDF.
- Integrate email sending with HTML invoice templates.

### Phase 3 — Polish & Observability

#### Observability
- Add Micrometer metrics for key business events (invoices created, payments received, reminders sent).
- Add a request correlation filter (`traceId` / `spanId` propagation across logs).
- Add request/response logging filter (logged asynchronously to avoid blocking).
- Configure `spring-boot-starter-actuator` with Prometheus + Grafana export.

#### Developer Tooling
- Add `spring-boot-devtools` is already there — good.
- Add a `Makefile` or `justfile` for common dev commands (`make run`, `make test`, `make docker-build`).
- Add `.editorconfig` for consistent formatting across the team.
- Configure `maven-compiler-plugin` with strict source/target and `-Werror` in CI.
- Add `checkstyle` or `spotless` with the Google/Adopted style guide — enforce formatting in CI.
- Add `archunit` for architecture tests (e.g., "no controller calls repository directly").
- Add a `README.md` with: local setup, env vars, common tasks, architecture overview.

#### Docker
- Add a multi-stage `Dockerfile` (build stage + runtime stage) for smaller final image.
- Secure the compose.yaml: use `${POSTGRES_PASSWORD:?err}` syntax, add health checks.
- Add a separate `docker-compose.dev.yaml` for local development with bind mounts.

---

## Quick Wins (Low Effort, High Value)

1. **`spring-boot-starter-validation`** + `@Valid` on all `@RequestBody` params — 5 min, blocks a whole class of attacks.
2. **Global `@ControllerAdvice` exception handler** — 15 min, makes all error responses consistent and safe (no stack traces in prod).
3. **`.env.example`** — 5 min, saves every new developer setup time.
4. **Structured JSON logging** (Logstash encoder via Logback config) — 10 min, makes log aggregation work properly.
5. **`springdoc-openapi`** — 10 min, gives frontend a live contract to work against.
6. **Flyway baseline migration** — creates `V1__init.sql` — 15 min, schema is now version-controlled.
7. **Add `@Version` to entities** — 5 min, prevents lost updates in concurrent scenarios.
8. **`mvn spring-boot:build-image`** in pom.xml plugin config — already supported, just needs the plugin configured.

---

## Suggested Dependency Additions

```xml
<!-- Security & Auth -->
<dependency>spring-boot-starter-security</dependency>
<dependency>jjwt-api, jjwt-impl, jjwt-jackson (0.12.x)</dependency>

<!-- Validation & Docs -->
<dependency>spring-boot-starter-validation</dependency>
<dependency>springdoc-openapi-starter-webmvc-ui</dependency>

<!-- Persistence -->
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>org.flywaydb:flyway-core</dependency>
<dependency>org.flywaydb:flyway-database-postgresql</dependency>

<!-- Observability -->
<dependency>spring-boot-starter-actuator</dependency>
<dependency>micrometer-registry-prometheus</dependency>
<dependency>net.logstash.logback:logstash-logback-encoder</dependency>

<!-- PDF Generation -->
<dependency>com.openhtmltopdf:openhtmltopdf-pdfbox</dependency>

<!-- Testing -->
<dependency>spring-security-test</dependency>
<dependency>org.testcontainers:junit-jupiter</dependency>
<dependency>org.testcontainers:postgresql</dependency>
<dependency>org.assertj:assertj-core</dependency>
```

---

## Summary Priority Order

1. **Security first** — Security + Validation + Spring Security (JWT) + Audit logging + HTTPS/CORS headers
2. **DX hygiene** — Global exception handler, structured logging, `.env.example`, profiles, Flyway migrations, OpenAPI docs
3. **Testing** — Add meaningful unit and integration tests with testcontainers
4. **Domain** — Entities, services, repositories, DTOs, enums, invoice number generation
5. **Polish** — PDF generation, email, actuator metrics, Docker multi-stage build, README
