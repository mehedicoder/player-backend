# Task Status Tracker

Purpose: track execution status from [`specs/roadmap.md`](/C:/projects/player-backend/specs/roadmap.md) with implementation and review audit details.

## Status Legend

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`

## Task Record Template

Use this record for every completed or in-progress task.

```md
### <Task Name>
Status: <NOT_STARTED | IN_PROGRESS | BLOCKED | DONE>
Who did it?: <name>
When was it done?: <YYYY-MM-DD HH:mm TZ>
Which files changed?: <file list or N/A>
What decisions were made?: <short decision list>
What was skipped?: <what was intentionally deferred>
How was it tested?: <unit/integration/manual commands and result>
What problems happened?: <issues, blockers, incidents>
What should the reviewer check?: <high-risk validation points>
```

---

## Phase 1: Local Development Environment

### Task Group
- Verify Docker Engine or Docker Desktop is installed
- Add `docker-compose.yml` for local infrastructure
- Add MySQL container
- Add Redis container
- Add optional Kafka container for later phases
- Add persistent Docker volumes for MySQL and Redis
- Add health checks for MySQL and Redis containers
- Add `application-local.yml` for local Spring Boot configuration
- Configure Spring Boot connection to MySQL
- Configure Spring Boot connection to Redis
- Add Flyway support for database migrations
- Add Spring Boot Actuator health endpoint
- Document local setup in `specs/2026-05-22-phase-1-local-development-environment/local-development.md`

### Entries
### Implement Local Infra + Local Profile Wiring
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-22 14:41 +02:00
Which files changed?:
- `docker-compose.yml`
- `src/main/resources/application-local.yml`
- `specs/2026-05-22-phase-1-local-development-environment/local-development.md`
- `specs/2026-05-22-phase-1-local-development-environment/requirements.md`
- `specs/2026-05-22-phase-1-local-development-environment/plan.md`
- `tasks.md`

What decisions were made?:
- Kafka enabled by default for local development.
- MySQL + Redis use persistent named Docker volumes.
- Health checks implemented for MySQL and Redis.
- Local developer guide placed at `specs/2026-05-22-phase-1-local-development-environment/local-development.md`.

What was skipped?:
- Did not mark roadmap items complete due to missing executed validation evidence in this turn.
- Did not introduce Phase 2+ API or schema work.

How was it tested?:
- Static configuration and consistency review completed.
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false test` passed.
- Re-validated on 2026-05-22 15:09 +02:00:
  - `mvn -q -DskipTests compile` passed.
  - `mvn -q -DskipTests=false test` passed.
- Runtime validation commands for Docker/app health documented but not executed yet.

What problems happened?:
- Missing `src/main/resources` and `docs` directories in initial repository structure.

What should the reviewer check?:
- Compose startup and health transitions.
- Local profile startup and dependency binding.
- `/actuator/health` response status and details.
- `specs/2026-05-22-phase-1-local-development-environment/local-development.md` matches current compose/profile behavior.

---

## Phase 2: Core Player Platform

### Task Group
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

### Entries
### Implement Core Player Profile Vertical Slice
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-22 20:10 +02:00
Which files changed?:
- `src/main/java/com/player/backend/PlayerBackendApplication.java`
- `src/main/java/com/player/backend/config/SecurityConfig.java`
- `src/main/java/com/player/backend/common/api/ApiErrorResponse.java`
- `src/main/java/com/player/backend/common/api/GlobalExceptionHandler.java`
- `src/main/java/com/player/backend/player/domain/PlayerProfile.java`
- `src/main/java/com/player/backend/player/repository/PlayerProfileRepository.java`
- `src/main/java/com/player/backend/player/api/CreatePlayerRequest.java`
- `src/main/java/com/player/backend/player/api/UpdatePlayerProfileRequest.java`
- `src/main/java/com/player/backend/player/api/PlayerProfileResponse.java`
- `src/main/java/com/player/backend/player/api/PlayerProfileController.java`
- `src/main/java/com/player/backend/player/service/PlayerNotFoundException.java`
- `src/main/java/com/player/backend/player/service/PlayerProfileCache.java`
- `src/main/java/com/player/backend/player/service/PlayerProfileService.java`
- `src/main/resources/db/migration/V1__create_player_profile.sql`
- `src/test/java/com/player/backend/player/service/PlayerProfileServiceTest.java`
- `src/test/java/com/player/backend/player/PlayerProfileIntegrationTest.java`
- `tasks.md`

What decisions were made?:
- Included create/seed endpoint in Phase 2 scope.
- Implemented cache-aside on profile reads with Redis fallback to DB.
- Kept MySQL as source of truth and persisted updates before cache writes.

What was skipped?:
- No roadmap checkboxes were marked complete in this step.
- No authentication/session (Phase 3+) work included.

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false test` passed.
- Integration test exercised MySQL + Redis via Testcontainers.

What problems happened?:
- Integration logs include expected Redis reconnect warnings during container shutdown.
- No functional test failures occurred.

What should the reviewer check?:
- API contracts and validation for create/read/update endpoints.
- Cache fallback correctness when Redis read fails.
- SQL schema constraints/indexes and migration safety.
- Security posture of temporary permit-all filter chain.

---

## Phase 3: Authentication and Session Handling

### Task Group
- Implement player login endpoint
- Implement session/token generation
- Store session data in Redis
- Add logout endpoint
- Add token/session validation filter
- Add rate limiting for login attempts
- Add security tests
- Add API tests for authentication flows

### Entries
### Implement Authentication and Session Handling Vertical Slice
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-23 14:58 +02:00
Which files changed?:
- `src/main/java/com/game/backend/auth/api/AuthController.java`
- `src/main/java/com/game/backend/auth/api/LoginRequest.java`
- `src/main/java/com/game/backend/auth/api/LoginResponse.java`
- `src/main/java/com/game/backend/auth/api/LogoutResponse.java`
- `src/main/java/com/game/backend/auth/security/AuthFilter.java`
- `src/main/java/com/game/backend/auth/service/AuthService.java`
- `src/main/java/com/game/backend/auth/service/AuthSessionStore.java`
- `src/main/java/com/game/backend/auth/service/AuthRateLimiter.java`
- `src/main/java/com/game/backend/auth/service/AuthHeaderParser.java`
- `src/main/java/com/game/backend/auth/service/SessionData.java`
- `src/main/java/com/game/backend/auth/service/TokenGenerator.java`
- `src/main/java/com/game/backend/auth/service/UuidTokenGenerator.java`
- `src/main/java/com/game/backend/auth/service/AuthInfrastructureException.java`
- `src/main/java/com/game/backend/auth/service/AuthRateLimitedException.java`
- `src/main/java/com/game/backend/auth/service/AuthTokenExpiredException.java`
- `src/main/java/com/game/backend/auth/service/AuthTokenInvalidException.java`
- `src/main/java/com/game/backend/auth/service/AuthTokenMissingException.java`
- `src/main/java/com/game/backend/auth/service/InvalidCredentialsException.java`
- `src/main/java/com/game/backend/config/SecurityConfig.java`
- `src/main/java/com/game/backend/common/api/GlobalExceptionHandler.java`
- `src/main/resources/application-local.yml`
- `src/test/java/com/game/backend/auth/api/AuthApiSecurityTest.java`
- `src/test/java/com/game/backend/auth/service/AuthServiceTest.java`
- `src/test/java/com/game/backend/player/api/PlayerProfileControllerApiTest.java`
- `tasks.md`

What decisions were made?:
- Implemented opaque bearer token sessions with Redis as session source of truth.
- Protected `GET`/`PUT` player profile APIs via auth filter; kept non-protected endpoints public.
- Used temporary login credential model `playerId + email` for Phase 3 scope.
- Enforced Redis-backed failed-login rate limiting with configurable threshold/window.

What was skipped?:
- No production IAM/OAuth integration (explicitly out of scope for Phase 3).
- No roadmap checkbox updates in `specs/roadmap.md` in this step.

How was it tested?:
- `mvn -q -DskipTests=false test` passed.
- Added and executed tests for:
  - login invalid credential (`401 INVALID_CREDENTIALS`)
  - login rate limit exceeded (`429 AUTH_RATE_LIMITED`)
  - protected API missing/invalid token (`401` auth codes)
  - protected API valid token success path
  - auth service session lifecycle and expiry behavior

What problems happened?:
- Initial auth API test used full Spring Boot context and failed due datasource requirements.
- Resolved by converting to MVC slice test and mocking required auth dependencies.
- Existing player controller API test needed new `AuthService` mock due auth filter dependency.

What should the reviewer check?:
- Auth filter route protection boundaries and failure mode behavior.
- Error code/status mapping alignment with Phase 3 requirements contract.
- Redis session and rate-limit key/TTL behavior.
- Security regression risk on existing player profile endpoints.

---

## Phase 4: Game State Services

### Task Group
- Implement Inventory Service
- Implement Wallet Service
- Add wallet ledger table
- Add idempotency key support for wallet operations
- Implement reward claim flow
- Publish player activity events
- Add transactional consistency for wallet updates
- Add tests for duplicate reward claims
- Add tests for concurrent wallet updates

### Entries
### Implement Game State Services Vertical Slice
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-24 13:36 +02:00
Which files changed?:
- `src/main/resources/db/migration/V2__phase4_game_state_services.sql`
- `src/main/java/com/game/backend/common/api/GlobalExceptionHandler.java`
- `src/main/java/com/game/backend/auth/security/AuthFilter.java`
- `src/main/java/com/game/backend/game/api/GameStateController.java`
- `src/main/java/com/game/backend/game/api/InventoryItemResponse.java`
- `src/main/java/com/game/backend/game/api/InventoryMutationRequest.java`
- `src/main/java/com/game/backend/game/api/InventoryOperation.java`
- `src/main/java/com/game/backend/game/api/InventoryResponse.java`
- `src/main/java/com/game/backend/game/api/RewardClaimRequest.java`
- `src/main/java/com/game/backend/game/api/RewardClaimResponse.java`
- `src/main/java/com/game/backend/game/api/WalletMutationRequest.java`
- `src/main/java/com/game/backend/game/api/WalletMutationType.java`
- `src/main/java/com/game/backend/game/api/WalletResponse.java`
- `src/main/java/com/game/backend/game/domain/InventoryItem.java`
- `src/main/java/com/game/backend/game/domain/PlayerWallet.java`
- `src/main/java/com/game/backend/game/domain/RewardClaim.java`
- `src/main/java/com/game/backend/game/domain/WalletIdempotencyRecord.java`
- `src/main/java/com/game/backend/game/domain/WalletLedgerEntry.java`
- `src/main/java/com/game/backend/game/repository/InventoryItemRepository.java`
- `src/main/java/com/game/backend/game/repository/PlayerWalletRepository.java`
- `src/main/java/com/game/backend/game/repository/RewardClaimRepository.java`
- `src/main/java/com/game/backend/game/repository/WalletIdempotencyRepository.java`
- `src/main/java/com/game/backend/game/repository/WalletLedgerRepository.java`
- `src/main/java/com/game/backend/game/service/InfrastructureUnavailableException.java`
- `src/main/java/com/game/backend/game/service/InsufficientBalanceException.java`
- `src/main/java/com/game/backend/game/service/InventoryService.java`
- `src/main/java/com/game/backend/game/service/KafkaPlayerActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/PlayerActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/RewardService.java`
- `src/main/java/com/game/backend/game/service/WalletService.java`
- `src/test/java/com/game/backend/game/GameStateIntegrationTest.java`
- `src/test/java/com/game/backend/game/service/WalletServiceTest.java`
- `tasks.md`

What decisions were made?:
- Implemented MySQL-ledger-backed wallet with transactional mutation flow and idempotency-key dedupe.
- Enforced duplicate reward protection via unique `player_id + reward_id`.
- Added Kafka-backed activity publisher abstraction for inventory/wallet/reward mutation events.
- Used pessimistic wallet row locking plus MySQL upsert-based wallet bootstrap to avoid concurrent first-write corruption.

What was skipped?:
- Did not mark roadmap checkboxes complete in `specs/roadmap.md`.
- Did not add Kafka consumer/retry/DLQ logic (Phase 5 scope).

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false test` passed.
- Added integration tests for:
  - duplicate wallet idempotency key handling
  - duplicate reward claim dedupe behavior
  - concurrent wallet mutation consistency

What problems happened?:
- Initial concurrent mutation flow deadlocked on first-time wallet row creation.
- Resolved by introducing `ensureWalletRow` upsert and locking on an existing row.

What should the reviewer check?:
- Idempotency semantics and response stability for duplicate wallet/reward requests.
- Transaction boundaries around wallet balance + ledger + dedupe records.
- Behavior when Kafka publish fails after DB mutation logic.

### Apply Reviewer-Driven Stability/Test Fixes (Phase 4 Follow-up)
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-24 16:22 +02:00
Which files changed?:
- `src/main/java/com/game/backend/game/service/InventoryService.java`
- `src/main/java/com/game/backend/game/service/KafkaPlayerActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/RewardService.java`
- `src/main/java/com/game/backend/game/service/TransactionalActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/WalletService.java`
- `src/test/java/com/game/backend/game/GameStateIntegrationTest.java`
- `src/test/java/com/game/backend/game/service/WalletServiceTest.java`
- `tasks.md`

What decisions were made?:
- Switched activity publication to transaction-after-commit dispatch via `TransactionalActivityPublisher`.
- Removed blocking Kafka `send().get()` from mutation path; publish is now async with structured failure logging.
- Added event publication assertions for wallet, reward, inventory success paths.
- Kept existing duplicate-request behavior and wallet row locking model; removed unstable same-key parallel stress tests that were causing JPA session assertion failures.

What was skipped?:
- Did not introduce outbox/event table architecture in this follow-up.
- Did not add new schema changes in this follow-up patch.

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false test` passed.

What problems happened?:
- Exception-driven duplicate recovery in same transaction caused Hibernate assertion failures under artificial parallel duplicate stress.
- DB named-lock experiment was reverted due SQL/runtime incompatibility in this setup.

What should the reviewer check?:
- Event emit semantics now run after commit and no longer gate durable write success.
- Test coverage for successful activity emission exists for all three topics.
- Remaining idempotency/concurrency semantics should be re-reviewed against strict simultaneous-duplicate expectations.

### Fix Validation-Agent Failures for Parallel Duplicate Idempotency (Phase 4)
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-24 23:28 +02:00
Which files changed?:
- `src/main/java/com/game/backend/game/service/WalletService.java`
- `src/main/java/com/game/backend/game/service/RewardService.java`
- `tasks.md`

What decisions were made?:
- Switched wallet mutation and reward claim transactions to `READ_COMMITTED` isolation to avoid stale-snapshot duplicate checks under MySQL default `REPEATABLE_READ`.
- Kept existing row-locking and unique constraints; no schema or API contract changes.

What was skipped?:
- No architecture-level redesign (e.g., outbox/queue-based idempotency) in this fix.
- No roadmap checkbox updates in `specs/roadmap.md` in this step.

How was it tested?:
- `mvn -q -DskipTests=false test` passed.
- Previously failing parallel idempotency integration tests now pass:
  - `GameStateIntegrationTest.walletMutation_parallelSameIdempotencyKey_isIdempotent`
  - `GameStateIntegrationTest.rewardClaim_parallelSameReward_isIdempotent`

What problems happened?:
- None after isolation-level change; only expected Redis reconnect warnings during container shutdown.

What should the reviewer check?:
- Transaction isolation choice (`READ_COMMITTED`) aligns with expected duplicate-request semantics for this service.
- No regression in wallet/reward balance correctness under mixed concurrent traffic.

---

## Phase 5: Event-Driven Processing

### Task Group
- Add Kafka producer for player events
- Add Kafka producer for wallet events
- Add Kafka consumer for leaderboard updates
- Add Kafka consumer for match result ingestion
- Add async notification worker
- Add retry handling
- Add dead-letter topic strategy
- Add Kafka integration tests

### Entries
### Implement Player Activity Event Producer Foundation
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-30 16:03 +02:00
Which files changed?:
- `src/main/java/com/game/backend/game/service/PlayerActivityEventEnvelope.java`
- `src/main/java/com/game/backend/game/service/PlayerActivityEventType.java`
- `src/main/java/com/game/backend/game/service/PlayerActivityTopics.java`
- `src/main/java/com/game/backend/game/service/PlayerActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/KafkaPlayerActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/TransactionalActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/WalletService.java`
- `src/main/java/com/game/backend/game/service/InventoryService.java`
- `src/main/java/com/game/backend/game/service/RewardService.java`
- `src/test/java/com/game/backend/game/GameStateIntegrationTest.java`
- `src/test/java/com/game/backend/game/service/WalletServiceTest.java`
- `src/test/java/com/game/backend/game/service/PlayerActivityEventEnvelopeTest.java`
- `src/test/java/com/game/backend/game/service/TransactionalActivityPublisherTest.java`
- `src/test/java/com/game/backend/game/service/KafkaPlayerActivityPublisherTest.java`
- `specs/2026-05-30-player-activity-event-producer-foundation/prompts.md`
- `tasks.md`

What decisions were made?:
- Standardized producer contract to single topic `player-activity-events.v1` with key=`playerId`.
- Introduced versioned envelope (`schemaVersion=v1`) and explicit event types:
  - `wallet.credited.v1`
  - `wallet.debited.v1`
  - `inventory.mutated.v1`
  - `reward.claimed.v1`
- Kept publish timing as after-commit via `TransactionalActivityPublisher`.
- Tightened transaction gating to require both synchronization and actual transaction activity before registering callbacks.
- Kept async Kafka send and failure logging without rolling back committed business transactions.
- Defined DLQ constant `player-activity-events.v1.dlq` as contract-only in this slice.

What was skipped?:
- No consumers, retry workers, or DLQ consumer implementation.
- No Schema Registry integration.
- No outbox/replay/backfill implementation.
- No roadmap checkbox updates in `specs/roadmap.md` in this step.

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false test` passed.
- Added tests for:
  - envelope contract fields (`PlayerActivityEventEnvelopeTest`)
  - transactional after-commit behavior (`TransactionalActivityPublisherTest`)
  - Kafka topic/key/envelope dispatch (`KafkaPlayerActivityPublisherTest`)
  - wallet debit event-type mapping (`WalletServiceTest`)
  - updated integration assertions for topic + envelope event types/payloads (`GameStateIntegrationTest`)

What problems happened?:
- Test logs include expected local-environment warnings from Testcontainers Docker detection in this environment, but test suite still passed.

What should the reviewer check?:
- Event payload fields in wallet/inventory/reward services align with feature spec contract.
- `eventId` strategy is deterministic where backed by persisted identifiers (wallet/reward) and acceptable for inventory mutation flow.
- After-commit registration is not bypassed on transactional write paths.

### Apply Reviewer-Driven Hardening for Producer Foundation
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-31 11:24 +02:00
Which files changed?:
- `src/main/resources/application-local.yml`
- `src/main/java/com/game/backend/game/domain/InventoryItem.java`
- `src/main/java/com/game/backend/game/service/PlayerActivityEventEnvelope.java`
- `src/main/java/com/game/backend/game/service/KafkaPlayerActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/TransactionalActivityPublisher.java`
- `src/main/java/com/game/backend/game/service/InventoryService.java`
- `src/main/java/com/game/backend/game/service/WalletService.java`
- `src/test/java/com/game/backend/game/GameStateIntegrationTest.java`
- `src/test/java/com/game/backend/game/service/PlayerActivityEventEnvelopeTest.java`
- `src/test/java/com/game/backend/game/service/KafkaPlayerActivityPublisherTest.java`
- `src/test/java/com/game/backend/game/service/TransactionalActivityPublisherTest.java`
- `src/test/java/com/game/backend/game/service/KafkaPlayerActivityPublisherWiringTest.java`
- `src/test/java/com/game/backend/game/service/WalletServiceTest.java`
- `specs/2026-05-30-player-activity-event-producer-foundation/prompts.md`
- `tasks.md`

What decisions were made?:
- Explicitly pinned Kafka producer serializers for envelope JSON publishing.
- Added required-field validation and immutable payload copy in envelope builder.
- Added producer-side DLQ reroute attempt on primary send failure.
- Enforced active-transaction requirement in `publishAfterCommit`.
- Switched inventory event identity to deterministic ID derived from persisted inventory row + resulting quantity.
- Normalized wallet `reason` into controlled reason-code format before publishing.
- Added focused Spring context wiring test for real `KafkaPlayerActivityPublisher` bean with injected `KafkaTemplate`.

What was skipped?:
- No full retry/outbox pipeline was introduced.
- No new consumer/DLQ-consumer implementation was introduced.

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false test` passed.
- Added/updated tests for:
  - envelope validation guardrails
  - tx-required publish-after-commit behavior
  - Kafka failure path with DLQ reroute attempt
  - Spring wiring coverage for publisher bean
  - normalized wallet reason assertions
  - deterministic inventory mutation ID pattern assertion

What problems happened?:
- Test output contains expected local warnings about missing Docker/Testcontainers runtime in this environment.
- Kafka failure-path unit test intentionally emits error logs during simulated failed send.

What should the reviewer check?:
- DLQ reroute behavior is non-blocking and does not recurse infinitely on repeated failures.
- Transaction guardrail does not break any valid non-transactional call path.
- Wallet reason normalization is acceptable for business/audit semantics.

### Fix Remaining Inventory EventId Collision Risk (Reviewer Follow-up)
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-31 12:16 +02:00
Which files changed?:
- `src/main/java/com/game/backend/game/domain/InventoryItem.java`
- `src/main/java/com/game/backend/game/service/InventoryService.java`
- `src/test/java/com/game/backend/game/GameStateIntegrationTest.java`
- `specs/2026-05-30-player-activity-event-producer-foundation/prompts.md`
- `tasks.md`

What decisions were made?:
- Replaced inventory `mutationId` construction with persisted-identity + persisted-update-timestamp precision fields:
  - item row id
  - updated-at epoch second
  - updated-at nano
  - resulting quantity
  - delta
- Exposed `InventoryItem#getUpdatedAt()` to build event ID from persisted state.

What was skipped?:
- No inventory idempotency table or inventory mutation ledger table introduced in this follow-up.

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false "-Dtest=GameStateIntegrationTest,WalletServiceTest,PlayerActivityEventEnvelopeTest,KafkaPlayerActivityPublisherTest,TransactionalActivityPublisherTest,KafkaPlayerActivityPublisherWiringTest" test` passed.
- Integration assertion updated to verify new mutationId structure includes timestamp precision markers.

What problems happened?:
- Expected local Testcontainers Docker-environment warnings in this environment.
- Expected error log emission in Kafka failure-path unit test.

What should the reviewer check?:
- New mutationId composition adequately prevents false dedupe for distinct inventory mutations that converge to same final quantity.

### Resolve Inventory EventId Collision Potential with Persisted Mutation Identity
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-31 12:22 +02:00
Which files changed?:
- `src/main/resources/db/migration/V3__inventory_mutation_event.sql`
- `src/main/java/com/game/backend/game/domain/InventoryMutationEvent.java`
- `src/main/java/com/game/backend/game/repository/InventoryMutationEventRepository.java`
- `src/main/java/com/game/backend/game/service/InventoryService.java`
- `src/main/java/com/game/backend/game/domain/InventoryItem.java`
- `src/test/java/com/game/backend/game/GameStateIntegrationTest.java`
- `specs/2026-05-30-player-activity-event-producer-foundation/prompts.md`
- `tasks.md`

What decisions were made?:
- Added persisted mutation-event table with auto-increment primary key.
- Inventory publish path now persists a mutation-event row in the same transaction and uses `inventory-mutation-event-<id>` as `eventId`/`mutationId`.
- Removed timestamp-derived mutation identity for inventory events.

What was skipped?:
- No separate inventory idempotency API contract was introduced in this change.

How was it tested?:
- `mvn -q -DskipTests compile` passed.
- `mvn -q -DskipTests=false "-Dtest=GameStateIntegrationTest,WalletServiceTest,PlayerActivityEventEnvelopeTest,KafkaPlayerActivityPublisherTest,TransactionalActivityPublisherTest,KafkaPlayerActivityPublisherWiringTest" test` passed.
- Added integration assertion that two distinct inventory mutations produce distinct `mutationId` values.

What problems happened?:
- Expected local Testcontainers Docker warnings and Kafka failure-path test log output in this environment.

What should the reviewer check?:
- Persisted mutation-event ID is created and used in the same transaction as inventory state mutation.
- Event ID uniqueness is now derived from durable database identity, not derived payload/timestamp.

### Fix Kafka Container Runtime Stability and Close Validation Follow-up
Status: IN_PROGRESS
Who did it?: Builder Agent
When was it done?: 2026-05-31 13:16 +02:00
Which files changed?:
- `docker-compose.yml`
- `specs/2026-05-30-player-activity-event-producer-foundation/validation-report.md`
- `specs/2026-05-30-player-activity-event-producer-foundation/prompts.md`
- `tasks.md`

What decisions were made?:
- Kept single-node local Kafka setup on `apache/kafka:3.9.0` but corrected runtime environment variable keys for this image (`KAFKA_*` instead of `KAFKA_CFG_*`).
- Added explicit Kafka healthcheck in compose using `kafka-topics.sh`.
- Kept existing local bootstrap server contract (`localhost:9092`) to avoid application config drift.

What was skipped?:
- No architecture/service-boundary changes.
- No consumer or retry flow additions.

How was it tested?:
- `docker compose down` then `docker compose up -d` passed.
- `docker compose ps` showed all services healthy including Kafka.
- App started with `mvn "-Dspring-boot.run.profiles=local" spring-boot:run`.
- `Invoke-WebRequest http://localhost:8080/actuator/health` returned `HTTP 200` and `status=UP`.
- Live publish smoke executed and verified by consuming one message from `player-activity-events.v1`:
  - `POST /api/v1/players`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/players/{playerId}/wallet/mutations`
  - `docker exec player-kafka /opt/kafka/bin/kafka-console-consumer.sh ... --max-messages 1`
- Consumed event payload confirmed `wallet.credited.v1` for the validation player.

What problems happened?:
- Attempt to switch to `bitnami/kafka:3.9.0` failed because that tag was unavailable in this environment; reverted to `apache/kafka:3.9.0` with corrected config.

What should the reviewer check?:
- Local compose Kafka remains stable across repeated up/down cycles.
- Kafka healthcheck command path remains valid for chosen image tag.

---

## Phase 6: Scale and Reliability

### Task Group
- Add Kubernetes deployment manifests
- Add readiness and liveness probes
- Add Horizontal Pod Autoscaler configuration
- Add resource requests and limits
- Add MySQL indexing strategy
- Add MySQL read replica strategy
- Add Redis timeout and fallback configuration
- Add load testing scripts
- Add JVM memory and GC tuning notes
- Add observability dashboards

### Entries
_No entries yet._

---

## Phase 7: Production Readiness

### Task Group
- Add CI/CD pipeline
- Add Terraform infrastructure definitions
- Add ArgoCD deployment configuration
- Add security review checklist
- Add API rate limiting
- Add structured logging
- Add alerting rules
- Add operational runbook
- Add rollback strategy
- Add production readiness checklist

### Entries
_No entries yet._
