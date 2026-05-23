# Validation - Phase 2 Core Player Platform

## Automated

Required:
1. `mvn -q -DskipTests compile`  
   - Must pass.
2. `mvn -q -DskipTests=false test`  
   - Must pass with no failing tests.

If integration tests are separated:
3. Run integration test suite for MySQL + Redis behavior.  
   - Must pass create/read/update and Redis fallback scenarios.

## Manual

1. Start dependencies:
   - `docker compose up -d`
2. Start app with local profile:
   - `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`
3. Create/seed player:
   - call create/seed endpoint and verify persisted row in MySQL.
4. Read player profile:
   - verify response matches MySQL source-of-truth.
5. Update player profile:
   - verify updated values are persisted and returned.
6. Cache behavior check:
   - repeat read calls to confirm optional minimal cache path does not alter correctness.
7. Redis failure behavior:
   - stop Redis temporarily and confirm read/update APIs still function via DB-first path.
8. OpenAPI check:
   - verify create/read/update endpoints are visible in API docs.

## Definition of Done

- All included Phase 2 tasks in `specs/2026-05-22-phase-2-core-player-platform/requirements.md` are implemented.
- Acceptance criteria for Phase 2 in `specs/roadmap.md` are met, including approved create/seed extension.
- `create`, `read`, and `update` flows are validated against MySQL persistence.
- Redis unavailability does not break core player profile operations.
- OpenAPI docs for implemented endpoints are available and accurate.
