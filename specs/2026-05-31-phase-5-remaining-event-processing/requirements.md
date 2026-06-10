# Requirements - Phase 5 Remaining Event Processing Slice

## Scope

This slice defines the remaining Phase 5 work after player-activity producer and leaderboard consumer foundations:

- Kafka consumer for match result ingestion
- Async notification worker flow
- Standard retry handling policy for these consumers/workers
- Dead-letter topic routing strategy
- Kafka integration test coverage for new flows

### In Scope

- Consume match result events from a dedicated topic (v1 contract)
- Validate and apply match-result side effects idempotently
- Emit or enqueue notification work asynchronously from consumed events
- Add retry/error classification for transient vs non-retriable failures
- Route unrecoverable or retry-exhausted records to DLQ
- Add integration tests that verify success, duplicate, retry, and DLQ behaviors

### Out of Scope

- Real-time websocket push delivery to end-clients
- New external notification providers (email/SMS/push SaaS integration)
- Replay tooling/UI for DLQ messages
- Cross-region/multi-cluster Kafka failover design
- New microservices or new infrastructure components

## Remaining Task Group Mapping (Roadmap Phase 5)

Covered by this feature-spec:

- `Add Kafka consumer for match result ingestion`
- `Add async notification worker`
- `Add retry handling`
- `Add dead-letter topic strategy`
- `Add Kafka integration tests`

Already handled in prior Phase 5 slices:

- `Add Kafka producer for player events`
- `Add Kafka producer for wallet events`
- `Add Kafka consumer for leaderboard updates`

## Functional Requirements

1. Match Result Consumer
   - Consume from `match-results.v1` (or human-approved equivalent topic name).
   - Require event envelope fields: `eventId`, `eventType`, `playerId`, `occurredAt`, `payload`.
   - Support v1 event type set for this slice:
     - `match.result.recorded.v1`
   - Reject malformed/unsupported events as non-retriable.
   - V1 identity model is **single-player event** only. Multi-participant payload contracts are out of scope for this slice.
   - Required payload fields for `match.result.recorded.v1`:
     - `matchId` (non-empty string)
     - `result` (enum: `WIN`, `LOSS`, `DRAW`)
     - `scoreDelta` (integer, may be negative/positive/zero, bounded to `[-1000, 1000]`)
   - Missing/invalid required fields are non-retriable and must be skip+commit with structured warning logs.

2. Idempotent Processing
   - Use durable dedupe keyed by `(consumerGroup, eventId)`.
   - Duplicate deliveries must not re-apply side effects.
   - Dedupe record and domain updates must be in one transaction boundary.

3. Notification Worker
   - Create a **durable notification job** from successfully processed match-result events.
   - Job persistence is in MySQL (same service, Flyway-managed schema) with status lifecycle:
     - `PENDING` -> `PROCESSING` -> `SENT`
     - `PENDING|PROCESSING` -> `RETRY_WAIT`
     - `RETRY_WAIT` -> `PROCESSING`
     - `RETRY_WAIT|PROCESSING` -> `FAILED_PERMANENT`
   - Notification job creation must not be dropped silently; job creation and match-result durability must be atomic in one transaction.
   - Downstream dispatch execution is asynchronous and must not roll back already committed ingestion state.
   - Failures in dispatch path must be observable (structured logs + metrics/counters).
   - Notification worker retry policy:
     - max attempts: `4` total attempts (initial + 3 retries)
     - backoff: exponential `1s`, `2s`, `4s`
     - on exhaustion: mark job `FAILED_PERMANENT` with last error metadata.

4. Retry + DLQ
   - Retries apply only to match-result consumer processing, using:
     - max attempts: `3` (initial + 2 retries)
     - backoff: exponential `500ms`, `1000ms`
   - Retries only for transient infrastructure/data access failures in these categories:
     - transient database connectivity/timeouts/lock acquisition failures
     - Kafka client/broker transient availability failures
     - transient transaction system failures
   - Non-retriable categories:
     - invalid envelope/payload
     - unsupported event type
     - deterministic domain validation errors
   - Retry-exhausted transient failures must route to DLQ topic `match-results.v1.dlq`.
   - Non-retriable messages are acknowledged/committed without retry loops.
   - DLQ message contract (minimum fields):
     - `originalTopic`
     - `originalPartition`
     - `originalOffset`
     - `eventId`
     - `eventType`
     - `consumerGroup`
     - `errorCategory`
     - `errorMessage`
     - `failedAt`

5. Observability
   - Structured logs must include safe identifiers (`eventId`, `eventType`, consumer group, reason).
   - Add counters/metrics for: processed, duplicate, retried, non-retriable skipped, DLQ routed, notification sent, notification retry, notification failed permanent.

## Non-Functional Requirements

- At-least-once delivery semantics with idempotent side-effect application.
- No unbounded in-memory buffering in consumers/workers.
- Bounded retry attempts and backoff.
- No secrets/sensitive payload dumps in logs.
- Preserve existing architecture boundaries and current stack.

## Decisions/Constraints

1. Keep implementation inside current Spring Boot service (no new service boundary).
2. Use existing persistence and migration strategy (Flyway + MySQL).
3. Keep retry policy explicit/configurable and consistent with existing Phase 5 consumer patterns.
4. Do not mark roadmap checkboxes complete until validation evidence exists.

## Artifact Placement Rule

All feature artifacts for this slice are stored under:

- `specs/2026-05-31-phase-5-remaining-event-processing/`

Expected report artifacts:

- `spec-review-report.md`
- `spec-validation-report.md`
- `review-report.md`
- `validation-report.md`
