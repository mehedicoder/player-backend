# Validation - Phase 5 Remaining Event Processing Slice

## Automated Validation

1. Compile:
   - `mvn -q -DskipTests compile`
2. Targeted tests for this slice:
   - `mvn -q -DskipTests=false "-Dtest=MatchResult*Test,Notification*Test,*Dlq*Test" test`
3. Full regression:
   - `mvn -q -DskipTests=false test`

## Required Assertions

1. Match Result Ingestion
   - Supported match-result event is consumed and processed successfully.
   - Invalid/malformed/unsupported events are treated as non-retriable and skipped with committed offset.

2. Idempotency
   - Re-delivery of identical `eventId` does not apply state changes twice.
   - Duplicate path is observable (counter/log) and does not fail consumer progress.

3. Retry Behavior
   - Match-result consumer transient failures are retried with exact policy:
     - max attempts `3` (initial + 2 retries)
     - backoff `500ms`, `1000ms`
   - Non-retriable failures are not retried.
   - Retriable classification matches spec categories (transient DB/Kafka/transaction-system failures only).

4. DLQ Strategy
   - Retry-exhausted transient failures are routed to `match-results.v1.dlq`.
   - DLQ record includes minimum required metadata:
     - `originalTopic`, `originalPartition`, `originalOffset`
     - `eventId`, `eventType`, `consumerGroup`
     - `errorCategory`, `errorMessage`, `failedAt`

5. Async Notification Worker
   - Successful match-result processing creates a durable notification job atomically in the same transaction boundary.
   - Notification worker retry policy is enforced:
     - max attempts `4` total
     - backoff `1s`, `2s`, `4s`
   - Retry exhaustion marks job `FAILED_PERMANENT` and emits structured observability signals.
   - Notification dispatch failure does not corrupt already committed ingestion state.

6. Regression Safety
   - Existing Phase 5 producer and leaderboard consumer tests continue passing.

## Manual Validation

1. Start local infra:
   - `docker compose up -d`
2. Start app with local profile:
   - `mvn "-Dspring-boot.run.profiles=local" spring-boot:run`
   - success signal: app startup completed and `/actuator/health` returns `HTTP 200` with `status=UP`
   - check command: `curl http://localhost:8080/actuator/health`
3. Produce valid `match.result.recorded.v1` event and verify:
   - ingestion side effects
   - notification work created/executed.
4. Re-send same `eventId` and verify idempotent no-op behavior.
5. Produce invalid event (missing `playerId` or invalid payload fields) and verify skip+commit semantics.
6. Force transient consumer-path fault and verify retries with `500ms`, `1000ms`, then DLQ route on exhaustion.
7. Force notification dispatch transient fault and verify worker retries with `1s`, `2s`, `4s`, then `FAILED_PERMANENT`.
8. Confirm logs/metrics counters increment across processed/duplicate/retry/dlq/notification-retry/notification-failed paths.

## Edge Cases

- Out-of-order match-result events.
- Consumer restart during retry windows.
- High duplicate burst for same event key.
- Notification worker transient failure while ingestion succeeds.
- Invalid `scoreDelta` bounds and invalid `result` enum values.

## Definition of Done

- Remaining Phase 5 task-group implementation completed with tests.
- Retry and DLQ behaviors demonstrated by automated evidence.
- Idempotent ingestion and async notification behavior validated.
- No unauthorized architecture or infrastructure changes introduced.
