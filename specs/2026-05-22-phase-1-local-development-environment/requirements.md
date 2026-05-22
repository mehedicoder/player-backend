# Requirements - Phase 1 Local Development Environment

## Scope

Included (strictly from `specs/roadmap.md` Phase 1):
- Verify Docker Engine or Docker Desktop is installed
- Add `docker-compose.yml` for local infrastructure
- Add MySQL container
- Add Redis container
- Add Kafka container (enabled by default for local)
- Add persistent Docker volumes for MySQL and Redis
- Add health checks for MySQL and Redis containers
- Add `application-local.yml` for local Spring Boot configuration
- Configure Spring Boot connection to MySQL
- Configure Spring Boot connection to Redis
- Add Flyway support for database migrations
- Add Spring Boot Actuator health endpoint
- Document local setup in `specs/2026-05-22-phase-1-local-development-environment/local-development.md`

Excluded:
- Any Phase 2+ feature work (player profile APIs, auth, wallet, inventory, leaderboards)
- Kubernetes/Terraform/ArgoCD setup
- Production CI/CD and production runbooks

## Decisions

1. Kafka is enabled by default in local environment.
Reason: explicit user instruction for this spec.

2. Flyway is the migration tool for this phase.
Reason: aligned with `specs/tech-stack.md`.

3. Feature-specific documentation path uses `specs/2026-05-22-phase-1-local-development-environment/local-development.md`.
Reason: feature-specific files stay inside their feature-spec directory.

## Context

- Spec style: execution-ready and concise.
- Stack constraints: Java 17+, Spring Boot, Maven, MySQL, Redis, Kafka, Flyway, Docker, Actuator.
- No new dependencies should be introduced without explicit approval.
