# Requirements - Phase 2 Core Player Platform

## Scope

Included (from `specs/roadmap.md` Phase 2 plus approved extension):
- Create initial player database schema
- Add Flyway migration for player tables
- Implement Player Profile Read API
- Implement Player Profile Update API
- Add Redis cache-aside strategy for hot player profile reads
- Add Redis fallback behavior if Redis is unavailable
- Add request validation
- Add global exception handling
- Add unit tests
- Add integration tests for MySQL and Redis
- Add API documentation using OpenAPI
- Add player create/seed API now (approved scope extension)

Excluded:
- Authentication/session handling from Phase 3
- Inventory/wallet/reward flows from Phase 4
- Kafka async workflows from Phase 5
- Production deployment automation work from later phases

## Decisions

1. Scope decision: include player create/seed API in this feature.
Reason: user selected extended scope for Phase 2.

2. Cache decision: DB-first with minimal caching in this phase.
Reason: user selected `DB-first` option to reduce cache complexity and risk during first vertical slice.

3. Source-of-truth decision: MySQL remains authoritative for player profile state.
Reason: aligned with roadmap objective and tech stack.

## Context

- Style: execution-ready and concise.
- Stack constraints: Java 17+, Spring Boot, Maven, MySQL, Flyway, Redis, OpenAPI, JUnit/Mockito/Testcontainers/REST Assured.
- No new dependencies without explicit approval.

## Artifact Placement Rule

Feature-specific artifacts for this phase must stay under:
- `specs/2026-05-22-phase-2-core-player-platform/`

Required feature-specific files in this folder:
- `requirements.md`
- `plan.md`
- `validation.md`
- future phase-specific review/validation reports and notes
