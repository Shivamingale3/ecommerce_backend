# Invoice Platform — Implementation Plan

## Overview

This plan transforms the bare Spring Boot 4.0.5 skeleton into a production-ready backend for the invoice automation platform. The plan is divided into **5 phases** in execution order. Each phase is self-contained and results in a working, mergeable increment.

---

## Phase 1 — Project Foundation & Configuration Hygiene

**Goal:** Establish project structure, configuration, logging, Docker, and global error handling. No business logic yet.

### 1.1 Migrate `application.properties` → `application.yml`

Rename `src/main/resources/application.properties` to `application.yml` and restructure:

```yaml
spring:
  application:
    name: invoice-platform
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  config:
    import: optional:file:./.env
```

Create profile files:
- `application-dev.yml` — local defaults, debug logging, no SSL
- `application-staging.yml` — reduced logging, partial SSL
- `application-prod.yml` — JSON structured logging, full SSL, minimal actuator exposure

### 1.2 Add `.env.example`

Document every env var the application reads:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/invoice_db
SPRING_DATASOURCE_USERNAME=invoice_user
SPRING_DATASOURCE_PASSWORD=change_me

# JWT
JWT_SECRET=change_me_minimum_256_bit_secret
JWT_EXPIRATION_MS=86400000

# Email
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=

# App
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

### 1.3 Add `Logback` XML config for structured JSON logging

- Use `logstash-logback-encoder` for JSON output in all non-dev profiles.
- Log pattern for dev: human-readable with color.
- Log pattern for prod: JSON with `traceId`, `spanId`, `level`, `logger`, `message`, `timestamp`.
- Configure async appender wrapping the console appender (non-blocking).

### 1.4 Add Global Exception Handler

Create `src/main/java/com/shivamingale/invoice/exception/GlobalExceptionHandler.java`:

| Exception | HTTP Status | Response Body |
|-----------|-------------|---------------|
| `MethodArgumentNotValidException` | 400 | `{ "status": 400, "error": "Validation Failed", "fieldErrors": [...] }` |
| `EntityNotFoundException` | 404 | `{ "status": 404, "error": "Not Found", "message": "Invoice not found with id 5" }` |
| `BusinessRuleViolationException` | 422 | `{ "status": 422, "error": "Business Rule Violation", "message": "..." }` |
| `AccessDeniedException` | 403 | `{ "status": 403, "error": "Forbidden" }` |
| `UnhandledThrowable` | 500 | Log full stack trace; return `{ "status": 500, "error": "Internal Server Error", "traceId": "..." }` |

Always include:
- `traceId` (from `MDC` or `ServerRequestContext`)
- `timestamp`
- `path`

Never leak stack traces to the client in production.

### 1.5 Add Request Correlation Filter

Create `src/main/java/com/shivamingale/invoice/config/RequestLoggingFilter.java` (or use Spring's `CommonsRequestLoggingFilter`):

- Read or generate a `traceId` from `X-Trace-Id` header (fallback: UUID).
- Inject into `MDC`.
- Log incoming request: method, URI, remote IP, traceId.
- Log outgoing response: status code, durationms, traceId.
- Add `traceId` to every response header (`X-Trace-Id`).

### 1.6 Add `application-local.yml` for Docker Compose

Update `compose.yaml` to use environment variable references:

```yaml
services:
  postgres:
    image: 'postgres:16-alpine'
    environment:
      POSTGRES_DB: ${POSTGRES_DB:invoice_db}
      POSTGRES_USER: ${POSTGRES_USER:invoice_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:change_me}
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_PROFILES_ACTIVE: docker
      # ... other env vars from .env
```

### 1.7 Add Docker Multi-Stage `Dockerfile`

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests -Pprod

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Xms256m", "-Xmx512m", "app.jar"]
```

### 1.8 Add `archetype` / project conventions

- Add `.editorconfig` — brace style, charset, indent style for all IDEs.
- Add `spotless-maven-plugin` (with Google Java Format) to `pom.xml` and configure a `mvn spotless:apply` goal.
- Add `checkstyle-maven-plugin` with a basic ruleset.
- Add `maven-enforcer-plugin` to enforce Java 21, dependency convergence, and banned dependencies.

### Deliverables

- [ ] `application.yml` with profile inheritance
- [ ] `application-dev.yml`, `application-staging.yml`, `application-prod.yml`
- [ ] `.env.example`
- [ ] `logback-spring.xml`
- [ ] `GlobalExceptionHandler`
- [ ] `RequestLoggingFilter` / correlation
- [ ] Updated `compose.yaml`
- [ ] `Dockerfile` (multi-stage)
- [ ] `.editorconfig`
- [ ] `spotless` + `checkstyle` + `enforcer` in pom.xml

---

## Phase 2 — Security & Authentication

**Goal:** Lock down every endpoint with JWT authentication, role-based authorization, and defense-in-depth headers.

### 2.1 Add Dependencies

```xml
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-validation</dependency>
<dependency>io.jsonwebtoken:jjwt-api:0.12.5</dependency>
<dependency>io.jsonwebtoken:jjwt-impl:0.12.5</dependency>
<dependency>io.jsonwebtoken:jjwt-jackson:0.12.5</dependency>
<dependency>spring-boot-starter-actuator</dependency>
```

### 2.2 JWT Infrastructure

**`JwtTokenProvider`** — generates and validates JWTs:
- Sign with `HS256` (configurable to `RS256` via env var for multi-service setups)
- Claims: `sub` (userId), `email`, `roles[]`, `tenantId`, `iat`, `exp`
- Secret loaded from `JWT_SECRET` env var, minimum 256-bit

**`JwtAuthenticationFilter`** (`OncePerRequestFilter`):
- Extract token from `Authorization: Bearer <token>` header
- Validate signature and expiration
- Set `SecurityContextHolder` with `UsernamePasswordAuthenticationToken`

**`JwtTokenService`**:
- `generateToken(UserDetails)` — create token for login response
- `validateToken(String token)` — used in filter
- `getUserIdFromToken(String token)` — for refresh flows

### 2.3 Security Configuration

**`SecurityConfig`** (`@Bean SecurityFilterChain`):

```java
// Pseudocode structure
http
  .csrf(AbstractHttpConfigurer::disable) // REST API — CSRF handled via token in body/header
  .cors(cors -> cors.configurationSource(corsConfigurationSource())) // explicit origins only
  .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/actuator/**").hasRole("ADMIN")
    .requestMatchers("/api/v1/**").authenticated()
    .anyRequest().authenticated()
  )
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
  .headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(FrameOptionsHeadersWriter.FrameOptionValue.DENY)
    .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
    .httpStrictTransportSecurity(hsts -> hsts
      .includeSubDomains(true)
      .maxAgeInSeconds
    )
    .xssProtection(xss -> xss.disable())
  )
```

### 2.4 User & Auth Entities

**`User` entity**:
- `id` (UUID), `email` (unique), `password` (BCrypt), `firstName`, `lastName`, `roles` (`Set<Role>`), `tenantId`, `enabled`, `accountNonLocked`, `@Version`

**`Role` enum**: `ADMIN`, `USER`, `VIEWER`

**`Tenant` entity** (for multi-tenancy):
- `id` (UUID), `name`, `subscriptionPlan`, `maxInvoices`, `createdAt`

### 2.5 Auth Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/auth/register` | Create account → returns JWT |
| `POST` | `/api/v1/auth/login` | Authenticate → returns JWT + refreshToken |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |
| `POST` | `/api/v1/auth/logout` | Invalidate refresh token (store invalidated tokens in Redis or DB) |
| `GET` | `/api/v1/auth/me` | Return current user profile |

### 2.6 Audit Logging

Create `AuditEvent` entity:
- `id`, `userId`, `tenantId`, `action` (enum), `entityType`, `entityId`, `previousState` (JSON), `newState` (JSON), `ipAddress`, `userAgent`, `traceId`, `timestamp`

Create `AuditEventListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)`):
- Publish `EntityAuditEvent` after every `save()`, `update()`, `delete()` on domain entities
- Log user ID, entity changed, before/after values
- Store in `audit_events` table with partition by month (for financial compliance)

### 2.7 Multi-Tenancy Support

Use **row-level tenancy** via a `TenantContext`:

```java
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) { ... }
    public static UUID getTenantId() { ... }
    public static void clear() { ... }
}
```

- Add `tenantId` filter in all repository queries
- Add `@TenantsOnly` aspect that rejects cross-tenant access
- `JwtAuthenticationFilter` sets `TenantContext` from JWT claims after auth

### Deliverables

- [ ] JWT token provider, filter, service
- [ ] `SecurityConfig` with all headers, CORS, stateless sessions
- [ ] `User`, `Role`, `Tenant` entities with JPA auditing
- [ ] Auth endpoints: register, login, refresh, logout, me
- [ ] `BCryptPasswordEncoder` bean
- [ ] Audit event entity and listener
- [ ] Tenant context with filter
- [ ] Rate limiting on auth endpoints (manual counter or bucket4j)

---

## Phase 3 — Domain Layer & Persistence

**Goal:** Define the complete domain model with JPA, Flyway migrations, repositories, services, and DTOs.

### 3.1 Domain Entities

**Entity hierarchy:**

```
Tenant (owning all)
  └── User (belongs to Tenant)
  └── Client (belongs to Tenant)
        └── Invoice (belongs to Tenant, references Client)
              └── InvoiceLineItem (belongs to Invoice)
        └── Payment (belongs to Tenant, references Invoice)
        └── Reminder (belongs to Tenant, references Invoice)
  └── AuditEvent (belongs to Tenant)
```

**`Client`**:
- `id` (UUID), `tenantId`, `name`, `email`, `phone`, `address` (embedded `Address`), `taxId`, `notes`, `createdAt`, `updatedAt`, `@Version`

**`Invoice`**:
- `id` (UUID), `tenantId`, `invoiceNumber` (String, unique per tenant), `clientId`, `status` (`InvoiceStatus`), `issueDate`, `dueDate`, `subtotal`, `taxRate`, `taxAmount`, `totalAmount`, `currency` (`Currency`, default USD), `notes`, `terms`, `createdAt`, `updatedAt`, `@Version`
- `invoiceNumber` generated by `SequenceGenerator`: format `INV-{YYYY}-{6-digit zero-padded sequence}`

**`InvoiceLineItem`**:
- `id` (UUID), `invoiceId`, `description`, `quantity` (BigDecimal), `unitPrice` (BigDecimal), `amount` (BigDecimal = qty × price), `sortOrder`

**`InvoiceStatus` enum**: `DRAFT`, `SENT`, `VIEWED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`

**`Payment`**:
- `id` (UUID), `tenantId`, `invoiceId`, `amount` (BigDecimal), `paymentDate`, `paymentMethod` (`PaymentMethod`), `referenceNumber`, `notes`, `createdAt`

**`PaymentMethod` enum**: `BANK_TRANSFER`, `CREDIT_CARD`, `CASH`, `CHECK`, `OTHER`

**`Reminder`**:
- `id` (UUID), `tenantId`, `invoiceId`, `type` (`ReminderType`), `scheduledAt`, `sentAt`, `status` (`ReminderStatus`), `attempts`, `lastError`

**`ReminderType` enum**: `DUE_SOON` (3 days before), `OVERDUE_1` (day 1), `OVERDUE_7`, `OVERDUE_14`, `OVERDUE_30`

**`ReminderStatus` enum**: `PENDING`, `SENT`, `FAILED`, `CANCELLED`

### 3.2 JPA Conventions

- All entities: `@Entity`, `@Table`, `@Id` (UUID), `@GeneratedValue`, `@CreatedDate`, `@LastModifiedDate`
- All monetary amounts: `BigDecimal` (never `Double`)
- All dates for business events: `LocalDate` or `LocalDateTime` — no `java.util.Date`
- All entity IDs: `UUID`
- All `BigDecimal` fields: explicit precision/scale via `@Column(precision=19, scale=4)`
- Optimistic locking: `@Version` on every entity
- `TenantContextHolder.get().getTenantId()` injected via `@PrePersist` / `@PreUpdate` entity listener

### 3.3 Flyway Migrations

```
db/migration/
  V1__init_schema.sql       -- tenants, users, roles tables
  V2__clients.sql           -- clients table
  V3__invoices.sql          -- invoices + line_items
  V4__payments.sql          -- payments
  V5__reminders.sql         -- reminders
  V6__audit_events.sql      -- audit_events (partitioned)
  V7__invoice_sequence.sql  -- invoice_number_sequence per tenant
```

Each migration is reviewed and tested before merge.

### 3.4 Repositories

- Spring Data JPA repositories for each entity
- Custom queries: filter by tenantId on every method
- `InvoiceRepository.findByTenantIdAndInvoiceNumber` — for uniqueness validation
- `InvoiceRepository.findOverdueInvoices(LocalDate today)` — for scheduler
- `ReminderRepository.findPendingReminders(LocalDateTime now)` — for scheduler

### 3.5 Services

**`InvoiceService`**:
- `createInvoice(CreateInvoiceRequest)` → validates client exists, generates invoice number, saves
- `updateInvoice(UUID id, UpdateInvoiceRequest)` → validates status allows editing (DRAFT only)
- `sendInvoice(UUID id)` → transitions to SENT, triggers email + creates first reminder
- `cancelInvoice(UUID id)` → transitions to CANCELLED, cancels pending reminders
- `getInvoice(UUID id)` → returns invoice with line items
- `listInvoices(Pageable, InvoiceFilter)` → paginated, filterable by status/client/date

**`PaymentService`**:
- `recordPayment(UUID invoiceId, RecordPaymentRequest)` → creates Payment, updates Invoice status
- `getPaymentsForInvoice(UUID invoiceId)` → list payments

**`ClientService`**:
- `createClient`, `updateClient`, `getClient`, `listClients(Pageable)`

**`SequenceGenerator`**:
- Thread-safe sequence per tenant using `SELECT MAX(invoice_number) + 1` with pessimistic lock, or a dedicated `sequence` table with `UPDATE ... RETURNING`
- Format: `INV-{year}-{zeroPadded6}`

### 3.6 DTOs

Request DTOs (in `dto/request/`):
- `CreateInvoiceRequest`, `UpdateInvoiceRequest`, `CreateClientRequest`, `RecordPaymentRequest`, `LoginRequest`, `RegisterRequest`

Response DTOs (in `dto/response/`):
- `InvoiceResponse`, `InvoiceListResponse` (summary for list), `ClientResponse`, `PaymentResponse`, `AuthResponse` (token + user), `ErrorResponse`

Never return JPA entities directly in controllers.

### Deliverables

- [ ] All entities: Client, Invoice, InvoiceLineItem, Payment, Reminder, User, Tenant, AuditEvent
- [ ] All enums: InvoiceStatus, PaymentMethod, ReminderType, ReminderStatus, Role
- [ ] Flyway migrations V1–V7
- [ ] All repositories with tenant-scoped queries
- [ ] All services: InvoiceService, ClientService, PaymentService, ReminderService, SequenceGenerator
- [ ] All DTOs: request and response
- [ ] JPA auditing enabled (`@EnableJpaAuditing`)

---

## Phase 4 — API Layer & Business Features

**Goal:** Build all REST endpoints, PDF generation, email reminders, and scheduling.

### 4.1 REST Endpoints

**Prefix**: `/api/v1`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register` | public | Register |
| `POST` | `/auth/login` | public | Login |
| `POST` | `/auth/refresh` | public | Refresh token |
| `POST` | `/auth/logout` | authenticated | Logout |
| `GET` | `/auth/me` | authenticated | Current user |
| `GET` | `/clients` | authenticated | List clients (paginated) |
| `POST` | `/clients` | authenticated | Create client |
| `GET` | `/clients/{id}` | authenticated | Get client |
| `PUT` | `/clients/{id}` | authenticated | Update client |
| `DELETE` | `/clients/{id}` | authenticated | Delete client (soft delete) |
| `GET` | `/invoices` | authenticated | List invoices |
| `POST` | `/invoices` | authenticated | Create invoice |
| `GET` | `/invoices/{id}` | authenticated | Get invoice (full detail) |
| `PUT` | `/invoices/{id}` | authenticated | Update invoice (DRAFT only) |
| `DELETE` | `/invoices/{id}` | authenticated | Cancel invoice |
| `POST` | `/invoices/{id}/send` | authenticated | Send invoice (email PDF to client) |
| `POST` | `/invoices/{id}/payments` | authenticated | Record payment |
| `GET` | `/invoices/{id}/payments` | authenticated | List payments for invoice |
| `GET` | `/dashboard/summary` | authenticated | Totals: outstanding, overdue, paid this month |
| `GET` | `/dashboard/monthly-revenue` | authenticated | Monthly revenue for last 12 months |
| `GET` | `/dashboard/invoice-status-breakdown` | authenticated | Count by status |

### 4.2 Validation Rules (on DTOs)

| Field | Rule |
|-------|------|
| `CreateInvoiceRequest.clientId` | `@NotNull` |
| `CreateInvoiceRequest.lineItems` | `@NotEmpty`, each item `@Valid` |
| `CreateInvoiceRequest.dueDate` | `@Future` or `@FutureOrPresent` |
| `CreateInvoiceRequest.taxRate` | `@DecimalMin("0") @DecimalMax("100")` |
| `CreateClientRequest.email` | `@Email`, unique per tenant |
| `RecordPaymentRequest.amount` | `@Positive` |

### 4.3 PDF Generation

**`InvoicePdfService`**:

- Use **Thymeleaf** to render an HTML invoice template.
- Use **openhtmltopdf** (PDFBox renderer) to convert HTML → PDF.
- Template includes: company logo placeholder, invoice number, client details, line items table, subtotal/tax/total, payment terms, due date.
- PDF returned as `ResponseEntity<byte[]>` with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="INV-2026-000001.pdf"`.
- **Endpoint**: `GET /api/v1/invoices/{id}/pdf` — streams PDF to browser/download.

### 4.4 Email Service

**`EmailService`** (`InvoiceEmailService`):

- Use `JavaMailSender` with HTML Thymeleaf templates.
- **Email types**:
  - Invoice delivery email (HTML body with inline PDF attachment)
  - Payment received confirmation
  - Reminder emails (DUE_SOON, OVERDUE variants)
- Email templates stored in `src/main/resources/templates/email/`.

### 4.5 Scheduled Reminder Job

**`ReminderScheduler`** (`@Scheduled`):

- Runs every hour (configurable via `reminder.cron`).
- Queries `ReminderRepository.findPendingReminders(beforeNow)`.
- For each pending reminder:
  1. Load invoice → check status (skip if already PAID or CANCELLED).
  2. Generate PDF.
  3. Send email via `InvoiceEmailService`.
  4. Update reminder status to SENT with `sentAt`.
  5. If fails: increment `attempts`, set `lastError`, schedule next attempt (max 3 attempts).
- Use **ShedLock** (`@SchedulerLock`) to prevent concurrent job execution across multiple instances.

### 4.6 Data Visualization Endpoints

**`DashboardService`**:

- `getSummary(tenantId)` → total outstanding, total overdue, paid this month, total invoices
- `getMonthlyRevenue(tenantId, int monthsBack)` → `[{month: "2026-01", revenue: 12500.00}, ...]`
- `getInvoiceStatusBreakdown(tenantId)` → `{DRAFT: 5, SENT: 12, OVERDUE: 3, PAID: 45}`

These aggregate queries use JPA `@Query` with `GROUP BY` or a native query for performance.

### 4.7 OpenAPI Documentation

Add SpringDoc configuration:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
  security-schemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

Annotate controllers with `@Tag`, `@Operation`, and `@SecurityRequirement(name = "bearerAuth")`.

### Deliverables

- [ ] All REST endpoints with `@Valid` DTO binding
- [ ] PDF generation service and endpoint
- [ ] Email service with Thymeleaf templates
- [ ] Reminder scheduler with ShedLock
- [ ] Dashboard aggregation endpoints
- [ ] SpringDoc OpenAPI config with JWT security scheme
- [ ] Swagger UI at `/swagger-ui.html`

---

## Phase 5 — Testing & Observability

**Goal:** Comprehensive test coverage and production-ready operational features.

### 5.1 Test Dependencies

```xml
<dependency>spring-security-test</dependency>
<dependency>org.testcontainers:junit-jupiter</dependency>
<dependency>org.testcontainers:postgresql</dependency>
<dependency>org.assertj:assertj-core</dependency>
<dependency>com.h2database:h2</dependency>  <!-- for unit tests only -->
```

### 5.2 Test Profile

Create `application-test.yml`:
- Use H2 in-memory database
- Disable Flyway (`spring.flyway.enabled=false`) for unit tests
- Set `spring.jpa.hibernate.ddl-auto=create-drop`

### 5.3 Base Test Classes

**`BaseIntegrationTest`**:
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE) // use testcontainers
public abstract class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### 5.4 Unit Tests

| Class | What to Test |
|-------|-------------|
| `JwtTokenProviderTest` | Generate, validate, extract claims, expired token rejection |
| `InvoiceServiceTest` | Create, update, send, cancel, payment recording, status transitions |
| `SequenceGeneratorTest` | Thread-safe sequence generation, format correctness |
| `InvoicePdfServiceTest` | PDF content generation, file not null, correct filename |
| `ReminderSchedulerTest` | Reminder selection, status transitions, max attempt logic |
| `DashboardServiceTest` | Aggregation calculations |

### 5.5 Controller Tests (MockMvc)

Each controller gets a `@WebMvcTest` (or integration test) with:
- `MockMvc` with `print()` for debugging
- `with(user(...))` from `spring-security-test` for authenticated requests
- `csrf()` token for form-based (skip for REST with JWT)
- Assertions: HTTP status, response body structure, error format consistency

### 5.6 Architecture Tests (ArchUnit)

```java
@ArchTest
static final ArchRule controllersOnlyCallServices =
    classes()
        .that().resideInAPackage("..controller..")
        .should().onlyCallMethodsThat().resideInAPackage("..service..");

@ArchTest
static final ArchRule noEntityExposure =
    classes()
        .that().resideInAPackage("..dto.response..")
        .should().notBeAssignableTo(Entity.class);

@ArchTest
static final ArchRule noJPAInService =
    noMethods()
        .that().areDeclaredInClassesThat().resideInAPackage("..service..")
        .should().beAnnotatedWith(Transactional.class);
```

### 5.7 Actuator Endpoints

Configure in `application-prod.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
  health:
    db:
      enabled: true
    mail:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

### 5.8 Business Metrics (Micrometer)

Add custom meters in services:

```java
meterRegistry.counter("invoices.created", "tenant", tenantId.toString()).increment();
meterRegistry.counter("payments.recorded", "tenant", tenantId.toString()).increment();
meterRegistry.counter("reminders.sent", "tenant", tenantId.toString(), "type", type.name()).increment();
meterRegistry.timer("invoice.pdf.generation").record(duration, TimeUnit.MILLISECONDS);
```

### 5.9 README

Create `README.md` with:
- Architecture overview (1 diagram in ASCII or Mermaid)
- Prerequisites (Java 21, Docker, .env setup)
- Local development setup (step by step)
- Running tests
- Common tasks (generate PDF, send test email)
- Environment variables reference
- API documentation link
- Deployment notes
- Contributing guidelines

### Deliverables

- [ ] Testcontainers integration with `BaseIntegrationTest`
- [ ] Unit tests for all services and JwtTokenProvider
- [ ] Controller tests for all endpoints
- [ ] ArchUnit rules for layered architecture enforcement
- [ ] Actuator with Prometheus metrics
- [ ] Custom Micrometer business metrics
- [ ] `README.md` with full developer onboarding guide

---

## Implementation Order Summary

```
Phase 1  → Project config, logging, global error, Docker
Phase 2  → Security, JWT, auth endpoints, audit logging, multi-tenancy
Phase 3  → Domain entities, Flyway migrations, repositories, services, DTOs
Phase 4  → REST endpoints, PDF, email, reminders, dashboard, OpenAPI
Phase 5  → Tests, Actuator, metrics, README
```

Each phase should be a **separate branch + pull request** so changes are reviewable and reversible. Phases 1–3 are prerequisite for Phase 4. Phase 5 can run in parallel with Phase 4 testing.

---

## Files to Create (Complete清单)

```
src/main/java/com/shivamingale/invoice/
├── config/
│   ├── SecurityConfig.java
│   ├── JpaAuditingConfig.java
│   ├── OpenApiConfig.java
│   └── AsyncConfig.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtTokenService.java
│   ├── JwtAuthenticationFilter.java
│   ├── TenantContext.java
│   └── TenantFilter.java
├── entity/
│   ├── BaseEntity.java          # Abstract with id, createdAt, updatedAt
│   ├── Tenant.java
│   ├── User.java
│   ├── Client.java
│   ├── Invoice.java
│   ├── InvoiceLineItem.java
│   ├── Payment.java
│   ├── Reminder.java
│   └── AuditEvent.java
├── enums/
│   ├── Role.java
│   ├── InvoiceStatus.java
│   ├── PaymentMethod.java
│   ├── ReminderType.java
│   ├── ReminderStatus.java
│   └── Currency.java
├── repository/
│   ├── UserRepository.java
│   ├── ClientRepository.java
│   ├── InvoiceRepository.java
│   ├── PaymentRepository.java
│   ├── ReminderRepository.java
│   ├── TenantRepository.java
│   └── AuditEventRepository.java
├── service/
│   ├── AuthService.java
│   ├── InvoiceService.java
│   ├── ClientService.java
│   ├── PaymentService.java
│   ├── ReminderService.java
│   ├── ReminderScheduler.java
│   ├── SequenceGenerator.java
│   ├── InvoicePdfService.java
│   ├── InvoiceEmailService.java
│   ├── DashboardService.java
│   └── AuditService.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── CreateClientRequest.java
│   │   ├── UpdateClientRequest.java
│   │   ├── CreateInvoiceRequest.java
│   │   ├── UpdateInvoiceRequest.java
│   │   └── RecordPaymentRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── ClientResponse.java
│       ├── InvoiceResponse.java
│       ├── InvoiceListResponse.java
│       ├── PaymentResponse.java
│       ├── DashboardSummaryResponse.java
│       ├── MonthlyRevenueResponse.java
│       ├── InvoiceStatusBreakdownResponse.java
│       └── ErrorResponse.java
├── controller/
│   ├── AuthController.java
│   ├── ClientController.java
│   ├── InvoiceController.java
│   └── DashboardController.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BusinessRuleViolationException.java
│   └── UnauthorizedException.java
├── event/
│   ├── EntityAuditEvent.java
│   └── EntityAuditEventListener.java
└── util/
    └── InvoiceNumberFormatter.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-staging.yml
├── application-prod.yml
├── application-test.yml
├── application-docker.yml
├── logback-spring.xml
└── templates/
    └── email/
        ├── invoice-sent.html
        ├── payment-received.html
        └── reminder.html

db/migration/
├── V1__init_schema.sql
├── V2__clients.sql
├── V3__invoices.sql
├── V4__payments.sql
├── V5__reminders.sql
├── V6__audit_events.sql
└── V7__invoice_sequence.sql

src/test/java/com/shivamingale/invoice/
├── BaseIntegrationTest.java
├── security/
│   └── JwtTokenProviderTest.java
├── service/
│   ├── InvoiceServiceTest.java
│   ├── SequenceGeneratorTest.java
│   ├── DashboardServiceTest.java
│   └── ReminderSchedulerTest.java
├── controller/
│   ├── AuthControllerTest.java
│   ├── ClientControllerTest.java
│   ├── InvoiceControllerTest.java
│   └── DashboardControllerTest.java
└── architecture/
    └── ArchitectureTest.java

.env.example
Dockerfile
README.md
.editorconfig
compose.yaml          # updated
```
