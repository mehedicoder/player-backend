# Player Backend

Scalable backend platform for millions of game players.

## Mission

Build a highly available, low-latency, horizontally scalable backend supporting:
- Player registration and login
- Player profile management
- Inventory
- Wallet and rewards
- Match history
- Leaderboards
- Real-time and asynchronous event processing

## Tech Stack

- Java 17
- Spring Boot
- Maven
- MySQL + Flyway
- Redis
- Kafka
- Docker

## Repository Structure

- `specs/` - mission, roadmap, feature specs, validation/review artifacts
- `prompts/` - project prompt logs and role prompts
- `skills/` - local skill definitions
- `src/` - application source code
- `tasks.md` - task tracking and execution notes

## Current Status

Phase 1 (`2026-05-22-phase-1-local-development-environment`) has been implemented and reviewed/validated, with known blockers documented in:
- `specs/2026-05-22-phase-1-local-development-environment/review-report.md`
- `specs/2026-05-22-phase-1-local-development-environment/validation-report.md`

## Local Development (Phase 1)

Primary guide:
- `specs/2026-05-22-phase-1-local-development-environment/local-development.md`

Quick commands:

```bash
docker compose up -d
mvn -q -DskipTests compile
mvn -q -DskipTests=false test
```

## Notes

- Architecture and production decisions are human-owned (see `AGENTS.md`).
- Do not commit/push/merge automatically unless explicitly requested.
