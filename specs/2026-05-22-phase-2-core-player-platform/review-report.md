# Review Report - Phase 2 Core Player Platform

Feature: `specs/2026-05-22-phase-2-core-player-platform`  
Date: 2026-05-22  
Role: Reviewer Agent

## Findings

### 1) HIGH - Duplicate key/unique constraint errors currently return 500 instead of a client error

- Files:
  - [PlayerProfileService.java](/C:/projects/player-backend/src/main/java/com/player/backend/player/service/PlayerProfileService.java:47)
  - [GlobalExceptionHandler.java](/C:/projects/player-backend/src/main/java/com/player/backend/common/api/GlobalExceptionHandler.java:46)
- Issue:
  - `createPlayer` relies on DB constraints for duplicate email handling, but there is no explicit `DataIntegrityViolationException` (or equivalent) mapping.
  - Constraint failures will fall into generic `Exception` handler and return 500.
- Impact:
  - Client receives internal error for a predictable validation/business conflict.
  - Violates robust API error contract expectations for Phase 2.
- Recommendation:
  - Add explicit handler for `DataIntegrityViolationException` (or `ConstraintViolationException` from persistence layer) and return 409/400 with stable error code.

### 2) MEDIUM - Phase 2 tests validate service layer, but not REST API contracts end-to-end

- File: [PlayerProfileIntegrationTest.java](/C:/projects/player-backend/src/test/java/com/player/backend/player/PlayerProfileIntegrationTest.java:47)
- Issue:
  - Integration test calls `PlayerProfileService` directly instead of exercising HTTP endpoints.
  - Current coverage does not validate controller request/response mapping, status codes, validation errors, or global exception responses.
- Impact:
  - Acceptance criteria says profile read/update should work through REST API; this is not directly proven by tests.
- Recommendation:
  - Add API-level integration tests (`@SpringBootTest` + `TestRestTemplate`/`MockMvc` or REST Assured) for create/read/update, invalid payloads, invalid player ID, and not-found behavior.

### 3) LOW - Security config currently permits all endpoints globally

- File: [SecurityConfig.java](/C:/projects/player-backend/src/main/java/com/player/backend/config/SecurityConfig.java:17)
- Issue:
  - `anyRequest().permitAll()` opens every endpoint.
- Impact:
  - Acceptable for early phase bootstrapping, but this should be explicitly temporary and tightened in Phase 3.
- Recommendation:
  - Keep as temporary with a tracked follow-up in Phase 3 auth work (or add comment/todo in config).

## Open Questions / Assumptions

- Assumption: for Phase 2, unauthenticated access is intentionally temporary.
- Assumption: DB-first approach allows optional cache use for repeated reads as implemented.

## Change Summary

- Core Phase 2 vertical slice is present:
  - player schema + Flyway migration
  - create/read/update service and controller
  - Redis cache-aside with fallback path
  - request validation + global exception skeleton
  - unit and integration tests (service-level integration)
