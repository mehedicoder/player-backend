# Plan - Phase 1 Local Development Environment

## 1. Local Infrastructure (Docker Compose)

1.1 Create `docker-compose.yml` with services: `mysql`, `redis`, `kafka`.
1.2 Map local ports:
- MySQL -> `3306`
- Redis -> `6379`
- Kafka -> default local broker port
1.3 Add named volumes for MySQL and Redis persistence.
1.4 Add container health checks for MySQL and Redis.

## 2. Application Local Profile

2.1 Add `src/main/resources/application-local.yml`.
2.2 Configure Spring datasource for local MySQL.
2.3 Configure Spring Redis for local Redis.
2.4 Configure Kafka bootstrap server for local broker.
2.5 Enable Flyway for startup migrations.
2.6 Expose Actuator health endpoint.

## 3. Migration Wiring

3.1 Ensure Flyway dependency/config is active in application.
3.2 Ensure migration path is recognized (`db/migration`).
3.3 Validate app startup applies/validates migrations in local profile.

## 4. Documentation

4.1 Create `specs/2026-05-22-phase-1-local-development-environment/local-development.md`.
4.2 Document prerequisites and startup commands.
4.3 Document health verification commands and expected outputs.
4.4 Document common local failures and recovery steps.

## 5. Acceptance Validation

5.1 `docker compose up -d` starts all local infra.
5.2 MySQL reachable on `localhost:3306`.
5.3 Redis reachable on `localhost:6379`.
5.4 Spring Boot starts with `local` profile.
5.5 App connects to MySQL and Redis.
5.6 `/actuator/health` reports healthy status.
