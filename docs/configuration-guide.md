# Configuration Guide

This document explains every configuration file, every property, and every environment variable in the project — what it does, what values it accepts, and what happens if you change it.

---

## How Profiles Work

Spring Boot loads configuration in this order (later overrides earlier):

1. `application.yml` — base defaults (always loaded)
2. `application-{profile}.yml` — profile-specific overrides
3. Environment variables — highest priority, override YAML
4. Command-line arguments — highest priority, override everything

The active profile is set via `SPRING_PROFILES_ACTIVE` env var (default: `dev`).

---

## Files Overview

| File                      | Purpose                           | When It's Active                 |
| ------------------------- | --------------------------------- | -------------------------------- |
| `application.yml`         | Base config, all defaults         | Always                           |
| `application-dev.yml`     | Local dev machine                 | `SPRING_PROFILES_ACTIVE=dev`     |
| `application-docker.yml`  | Running inside Docker Compose     | `SPRING_PROFILES_ACTIVE=docker`  |
| `application-staging.yml` | Staging / pre-production          | `SPRING_PROFILES_ACTIVE=staging` |
| `application-prod.yml`    | Production                        | `SPRING_PROFILES_ACTIVE=prod`    |
| `application-test.yml`    | Running unit/integration tests    | `SPRING_PROFILES_ACTIVE=test`    |
| `.env`                    | Local env vars for Docker Compose | Loaded by `docker compose`       |
| `.env.example`            | Template for `.env`               | Never loaded by the app          |
| `compose.yaml`            | Docker Compose stack definition   | `docker compose up`              |
| `logback-spring.xml`      | Logging configuration             | Always                           |

---

## `application.yml` — Base Configuration

This is the root configuration. Every property here can be overridden by any profile or environment variable. The `${VAR:default}` syntax means "use env var `VAR`, falling back to `default` if not set."

### `spring.application.name`

```yaml
spring:
  application:
    name: invoice-platform
```

The application name used in:

- Spring Cloud / Eureka service registration
- Logstash/Datadog labels
- Metrics (`spring.application.name` tag)
- Browser tab title in Spring Boot error pages

**Values:** Any string. Default: `invoice-platform`

---

### `spring.config.import`

```yaml
spring:
  config:
    import: optional:file:./.env
```

Imports `.env` file (if present) as additional configuration sources. `optional:` means the app won't fail if the file doesn't exist.

> **Important:** `.env` is for local development only. In production, set real environment variables directly.

---

### Database — `spring.datasource`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/invoice_db}
    username: ${SPRING_DATASOURCE_USERNAME:invoice_user}
    password: ${SPRING_DATASOURCE_PASSWORD:change_me}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10 # Max connections in the pool
      minimum-idle: 2 # Always-keep connections
      idle-timeout: 30000 # Close idle connections after 30s
      connection-timeout: 30000 # Fail if connection not available in 30s
      max-lifetime: 1800000 # Recycle connections after 30 minutes
```

**HikariCP** is the connection pool used by Spring Boot by default.

| Property             | Default   | What it does                                    |
| -------------------- | --------- | ----------------------------------------------- |
| `maximum-pool-size`  | 10        | Maximum number of connections in the pool       |
| `minimum-idle`       | 2         | Minimum connections kept alive when idle        |
| `idle-timeout`       | 30000ms   | Close idle connections after this duration      |
| `connection-timeout` | 30000ms   | Wait this long for a connection before throwing |
| `max-lifetime`       | 1800000ms | Force-close connections after this age          |

> **Tuning tip:** Set `maximum-pool-size` to `(core_count * 2) + spindle_count` for OLTP workloads. For a typical 4-core machine: ~10-20 connections.

---

### JPA / Hibernate — `spring.jpa`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate # Default: validates schema, never changes it
    open-in-view: false # Prevents LazyInitializationException from controllers
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          time_zone: UTC # All timestamps stored in UTC
```

| `ddl-auto` Value | Behavior                                                     |
| ---------------- | ------------------------------------------------------------ |
| `none`           | Do nothing — schema must exist                               |
| `validate`       | Check schema matches entities — **safe, production default** |
| `update`         | Add missing tables/columns — **dev default**                 |
| `create`         | Drop and recreate on every startup — **testing only**        |
| `create-drop`    | Create on startup, drop on shutdown — **testing only**       |

> **`open-in-view: false`** — This prevents `LazyInitializationException` by failing fast rather than letting Hibernate lazily load data outside a transaction. Always keep this `false`.

---

### Flyway Migrations — `spring.flyway`

```yaml
spring:
  flyway:
    enabled: ${FLYWAY_ENABLED:false}
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

| Property              | Default                  | What it does                                                               |
| --------------------- | ------------------------ | -------------------------------------------------------------------------- |
| `enabled`             | `false`                  | Set `true` in staging/prod to run DB migrations                            |
| `locations`           | `classpath:db/migration` | Where to find SQL migration files                                          |
| `baseline-on-migrate` | `true`                   | Creates a `flyway_schema_history` baseline if DB exists but has no history |
| `validate-on-migrate` | `true`                   | Ensures migrations match deployed state                                    |

---

### Email / SMTP — `spring.mail`

```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:smtp.example.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:}
    password: ${SPRING_MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

| Property               | Default            | What it does                          |
| ---------------------- | ------------------ | ------------------------------------- |
| `host`                 | `smtp.example.com` | SMTP server hostname                  |
| `port`                 | `587`              | SMTP port (587 = STARTTLS, 465 = SSL) |
| `username`             | empty              | SMTP auth username                    |
| `password`             | empty              | SMTP auth password                    |
| `smtp.auth`            | `true`             | Require authentication                |
| `smtp.starttls.enable` | `true`             | Use STARTTLS encryption               |

> Leave username/password empty to **disable email features** (the app will log warnings but won't crash).

---

### Server — `server`

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
  error:
    include-message: never # Never expose error messages in prod
    include-stacktrace: never # Never expose stack traces in prod
    include-binding-errors: never
```

| Property                   | What it does                                                      |
| -------------------------- | ----------------------------------------------------------------- |
| `port`                     | TCP port the app listens on                                       |
| `context-path`             | URL prefix (e.g., `/api` makes all URLs start with `/api`)        |
| `encoding.force`           | Force UTF-8 on all HTTP requests/responses                        |
| `error.include-message`    | `never` / `always` / `on-param` — controls error message exposure |
| `error.include-stacktrace` | `never` / `always` / `on-param` — controls stack trace exposure   |

> Error message/stack exposure is `never` in the base config and overridden to `always` only in the `dev` profile.

---

### JWT — `jwt`

```yaml
jwt:
  secret: ${JWT_SECRET:change_me_you_must_set_a_256_bit_secret_minimum_in_production}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000} # 24 hours
  refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000} # 7 days
```

| Property                | Default        | What it does                                                          |
| ----------------------- | -------------- | --------------------------------------------------------------------- |
| `secret`                | placeholder    | HMAC-SHA signing key. **Must be ≥ 256 bits (32 chars) in production** |
| `expiration-ms`         | 86400000 (24h) | How long the access token is valid                                    |
| `refresh-expiration-ms` | 604800000 (7d) | How long the refresh token is valid                                   |

> Generate a secure secret: `openssl rand -base64 48`

---

### Invoice Sequence — `invoice`

```yaml
invoice:
  sequence-format: "INV-%04d-%06d" # e.g. INV-2026-000001
  sequence-cache-size: 100 # Batch size for sequence number generation
```

| Property              | Default         | What it does                                                    |
| --------------------- | --------------- | --------------------------------------------------------------- |
| `sequence-format`     | `INV-%04d-%06d` | Format string for invoice numbers (year + zero-padded sequence) |
| `sequence-cache-size` | 100             | Number of sequence numbers to pre-allocate per tenant           |

---

### Reminder Scheduler — `reminder`

```yaml
reminder:
  cron: "0 0 * * * *" # Every hour, on the hour
  max-attempts: 3 # Retry failed reminder emails up to 3 times
  enabled: ${REMINDER_ENABLED:true}
```

| Property       | Default       | What it does                                                                    |
| -------------- | ------------- | ------------------------------------------------------------------------------- |
| `cron`         | `0 0 * * * *` | Cron expression for the reminder job (6 fields: sec min hour day month weekday) |
| `max-attempts` | 3             | Maximum email send attempts before marking reminder as permanently failed       |
| `enabled`      | `true`        | Set `false` to disable the entire reminder scheduler                            |

> Cron format: `second minute hour day-of-month month day-of-week`
> `0 0 * * * *` = every hour at minute 0, second 0
> `0 0 9 * * MON-FRI` = 9:00 AM every weekday

---

### Actuator — `management`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when_authorized # Only admins see full health info
      probes:
        enabled: true # Kubernetes liveness/readiness probes
```

| Endpoint               | What it exposes                               |
| ---------------------- | --------------------------------------------- |
| `/actuator/health`     | Application health (UP/DOWN), DB connectivity |
| `/actuator/info`       | Info about the app (version, git commit)      |
| `/actuator/metrics`    | Micrometer metrics (in staging/prod)          |
| `/actuator/prometheus` | Prometheus-formatted metrics (prod only)      |

---

### Logging — `logging`

```yaml
logging:
  level:
    root: INFO
    com.shivamingale.ecom: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: WARN
```

Sets log levels per package. Profile-specific files override these.

---

## `application-dev.yml` — Development Profile

Activated when `SPRING_PROFILES_ACTIVE=dev`.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update # Automatically add new columns/tables — safe in dev
    show-sql: false # Don't print SQL to logs (too noisy)
  flyway:
    enabled: false # No migrations — Hibernate manages schema
  h2:
    console:
      enabled: true # H2 web console at /h2-console (dev only)

server:
  error:
    include-message: always # Show full error messages
    include-stacktrace: always # Show full stack traces
    include-binding-errors: always

logging:
  level:
    root: DEBUG
    com.shivamingale.ecom: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE # Parameter values in SQL
```

> **Dev workflow:** Run `./mvnw spring-boot:run` or from your IDE. No Docker needed. Uses H2 in-memory DB by default.

---

## `application-docker.yml` — Docker Compose Profile

Activated when `SPRING_PROFILES_ACTIVE=docker`.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update # Hibernate manages schema in docker dev
    show-sql: false
  flyway:
    enabled: false # Migrations handled separately

logging:
  level:
    root: INFO
    com.shivamingale.ecom: DEBUG
```

> Use `docker compose up` for this profile. Connects to the real PostgreSQL container, not H2.

---

## `application-staging.yml` — Staging Profile

Activated when `SPRING_PROFILES_ACTIVE=staging`.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate # Strict: don't let Hibernate change schema
    show-sql: false
  flyway:
    enabled: true # Run migrations from db/migration/

server:
  error:
    include-message: never # No error details leaked to clients
    include-stacktrace: never
    include-binding-errors: never

logging:
  level:
    root: INFO
    com.shivamingale.ecom: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    # Human-readable timestamp + level + logger + message

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics # Expose metrics endpoint for APM
```

---

## `application-prod.yml` — Production Profile

Activated when `SPRING_PROFILES_ACTIVE=prod`.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate # Strict schema validation
    show-sql: false
  flyway:
    enabled: true # Run Flyway migrations

server:
  error:
    include-message: never
    include-stacktrace: never
    include-binding-errors: never
  http2:
    enabled: true # HTTP/2 for better performance

logging:
  level:
    root: WARN
    com.shivamingale.ecom: INFO # Application logs at INFO
    org.springframework: WARN # Framework logs at WARN
    org.hibernate: WARN
  pattern:
    console: '{"timestamp":"...","level":"...","logger":"...","message":"...","thread":"..."}%n'
    # JSON format for log aggregation (ELK, Grafana Loki, Datadog)

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus # Prometheus scraping
  endpoint:
    health:
      show-details: when_authorized # Full details only for authenticated admins
      probes:
        enabled: true # Kubernetes health probes
  health:
    db:
      enabled: true # Check DB connectivity in health endpoint
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true # Record p50, p95, p99 for all HTTP requests
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99

invoice:
  sequence-cache-size: 1000 # Larger batch for production (fewer DB round-trips)
```

---

## `application-test.yml` — Test Profile

Activated when `SPRING_PROFILES_ACTIVE=test` (applied automatically by `@ActiveProfiles("test")`).

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
    username: sa # H2 default admin user
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop # Recreate schema for each test class
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false # No migrations in tests
  h2:
    console:
      enabled: false # No H2 console in tests

logging:
  level:
    root: WARN
    com.shivamingale.ecom: DEBUG
```

> Uses H2 in-memory database. Each test class gets a fresh schema (`create-drop`).

---

## `.env` — Local Environment Variables

**This file is gitignored. Never commit it.**

Loaded by Docker Compose. Sets environment variables inside the `app` and `postgres` containers.

### Docker Compose env vars

| Variable                    | Example Value       | What it does                                                |
| --------------------------- | ------------------- | ----------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`    | `docker`            | Activates `application-docker.yml`                          |
| `POSTGRES_DB`               | `invoice_db`        | Name of the PostgreSQL database to create                   |
| `POSTGRES_USER`             | `invoice_user`      | PostgreSQL username                                         |
| `POSTGRES_PASSWORD`         | `InvoiceLocal2026!` | PostgreSQL password                                         |
| `POSTGRES_EXPOSED_PORT`     | `5433`              | Host port mapped to container port 5432                     |
| `SERVER_PORT`               | `8080`              | Port the Spring Boot app listens on inside Docker           |
| `JWT_SECRET`                | `YWJj...`           | HMAC signing key for JWT tokens                             |
| `JWT_EXPIRATION_MS`         | `86400000`          | Access token lifetime in milliseconds                       |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000`         | Refresh token lifetime in milliseconds                      |
| `SPRING_MAIL_HOST`          | `smtp.example.com`  | SMTP server hostname                                        |
| `SPRING_MAIL_PORT`          | `587`               | SMTP port                                                   |
| `SPRING_MAIL_USERNAME`      | (empty)             | SMTP username (leave empty to disable email)                |
| `SPRING_MAIL_PASSWORD`      | (empty)             | SMTP password                                               |
| `FLYWAY_ENABLED`            | `false`             | Enable Flyway migrations (set `true` when migrations exist) |

### Variable syntax in compose.yaml

```yaml
${VAR:default}           # Use VAR, fall back to "default" if VAR is not set
${VAR:?required}         # Fail startup if VAR is not set (no default)
${VAR:-default}          # Same as : (colon) — use default if VAR is unset or empty
```

---

## `.env.example` — Template

Copy this to `.env` and fill in your values:

```bash
cp .env.example .env
```

All variables are documented with comments explaining:

- What the variable does
- What format/value it expects
- Whether it's required or has a default

---

## `compose.yaml` — Docker Compose Stack

### `postgres` service

```yaml
postgres:
  image: postgres:16-alpine # Alpine = smaller image, faster to pull
  environment:
    POSTGRES_DB: ${POSTGRES_DB:-invoice_db} # Database name
    POSTGRES_USER: ${POSTGRES_USER:-invoice_user} # Username
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?required} # Password (required)
  ports:
    - "${POSTGRES_EXPOSED_PORT:-5432}:5432" # Host:Container port
  volumes:
    - postgres_data:/var/lib/postgresql/data # Persist data across restarts
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ... -d ..."] # pg_isready checks DB is ready
    interval: 10s # Run health check every 10 seconds
    timeout: 5s # Fail if not responding in 5 seconds
    retries: 5 # Mark unhealthy after 5 consecutive failures
    start_period: 10s # Give DB 10 seconds to initialize before first check
  restart: unless-stopped # Restart unless explicitly stopped
  networks:
    - invoice-net # Isolated Docker network
```

### `app` service

```yaml
app:
  build:
    context: . # Build from project root
    dockerfile: Dockerfile
  ports:
    - "${SERVER_PORT:-8080}:8080"
  depends_on:
    postgres:
      condition: service_healthy # Don't start until postgres is healthy
  environment:
    SPRING_PROFILES_ACTIVE: docker # Use docker profile
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
    #                          ^^^^^^ Docker Compose service name = hostname
    JWT_SECRET: ${JWT_SECRET:?required} # Required — no secret defaults in prod
    FLYWAY_ENABLED: "true" # Run migrations in Docker
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 30s # App needs time to start and run migrations
  restart: unless-stopped
```

### Named volumes

```yaml
volumes:
  postgres_data:
    driver: local # Stored at /var/lib/docker/volumes/<project>_postgres_data
```

### Networks

```yaml
networks:
  invoice-net:
    driver: bridge # Default bridge network, isolated from other stacks
```

---

## `logback-spring.xml` — Logging Configuration

### Profile: `dev` / `default`

```
2026-04-14 18:22:17.416 INFO  com.shivamingale.ecom.AuthService - User logged in: shivam@example.com
```

Colored, human-readable output for easy debugging in the terminal.

### Profile: `staging`

```
2026-04-14 18:22:17.416 [main] INFO  c.s.i.service.AuthService - User logged in: shivam@example.com
```

Plain text with timestamp, thread, level, logger, and message. Suitable for file-based log aggregation.

### Profile: `prod`

```json
{
  "timestamp": "2026-04-14T18:22:17.416+05:30",
  "level": "INFO",
  "logger": "com.shivamingale.ecom.service.AuthService",
  "message": "User logged in: shivam@example.com",
  "thread": "main",
  "application": "invoice-platform"
}
```

Structured JSON with fields: `timestamp`, `level`, `logger`, `message`, `thread`, `application`, and any MDC values (`traceId`).

Also uses an **async appender** (queue of 512, non-blocking) to prevent logging from slowing down HTTP requests.

---

## Common Workflows

### Run locally (no Docker)

```bash
cp .env.example .env
# Edit .env — set JWT_SECRET to a real value
./mvnw spring-boot:run
# Activates: dev profile, H2 in-memory DB, localhost:5432
```

### Run with Docker Compose

```bash
cp .env.example .env
# Edit .env — set JWT_SECRET and POSTGRES_PASSWORD
docker compose up -d
# Activates: docker profile, PostgreSQL, health checks
```

### Run tests

```bash
./mvnw test
# Activates: test profile, H2 in-memory DB
```

### Deploy to staging

```bash
SPRING_PROFILES_ACTIVE=staging \
JWT_SECRET=<real-secret> \
SPRING_DATASOURCE_URL=jdbc:postgresql://staging-db:5432/invoice \
FLYWAY_ENABLED=true \
java -jar invoice.jar
```
