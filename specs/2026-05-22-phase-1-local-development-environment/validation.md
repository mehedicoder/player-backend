# Validation - Phase 1 Local Development Environment

## Automated

Required:
1. `mvn -q -DskipTests=false test`
   - Must pass with no failing tests.
2. `mvn -q -DskipTests compile`
   - Must pass to confirm local profile config does not break build wiring.

If integration tests are present for infra connectivity:
3. Run integration test suite with local dependencies available.
   - Must pass MySQL and Redis connectivity checks.

## Manual

1. Start infrastructure:
   - `docker compose up -d`
2. Check service status:
   - `docker compose ps`
   - Confirm MySQL and Redis show healthy status.
3. Start app with local profile:
   - `mvn spring-boot:run -Dspring-boot.run.profiles=local`
4. Check health endpoint:
   - `GET /actuator/health`
   - Expect overall `UP` and dependency health shown.
5. Basic connectivity checks:
   - Confirm app logs show successful MySQL datasource initialization.
   - Confirm app logs show successful Redis connection initialization.

## Definition of Done

- All Phase 1 tasks in `specs/roadmap.md` are implemented.
- All Phase 1 acceptance criteria are met exactly.
- `specs/2026-05-22-phase-1-local-development-environment/local-development.md` exists and is accurate.
- No scope from Phase 2+ is included.
