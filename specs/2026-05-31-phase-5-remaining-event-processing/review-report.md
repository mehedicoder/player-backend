# Implementation Review Report - 2026-05-31-phase-5-remaining-event-processing

## Review Scope

- Latest working-tree implementation for Phase 5 remaining event processing after Builder Agent review fixes.
- Match-result Kafka consumer retry/DLQ behavior.
- Match-result idempotency and projection persistence.
- Durable notification worker claim/dispatch/finalize flow.
- Changed tests, configuration, migration, and task tracking.
- JavaDoc compliance for changed public Java classes and public contract methods.

## Review Method

- Read project context, workflow, roadmap, active feature spec, previous implementation review report, and `tasks.md`.
- Inspected latest changed and untracked implementation files directly because most Phase 5 files are still untracked and not shown in `git diff --stat`.
- Checked changed tests for coverage of prior reviewer findings.

## Findings

### 1) HIGH - Concurrent valid events for the same player/match can be acknowledged and lost

**Where**
- `src/main/java/com/game/backend/game/service/MatchResultIngestionService.java:109`
- `src/main/java/com/game/backend/game/service/MatchResultIngestionService.java:122`
- `src/main/java/com/game/backend/game/service/MatchResultIngestionService.java:125`
- `src/main/resources/db/migration/V5__phase5_remaining_event_processing.sql:11`

**Issue**
- `upsertProjection()` reads by `(playerId, matchId)` and then inserts/updates the projection.
- The DB enforces `UNIQUE (player_id, match_id)`.
- If two distinct valid events for the same `playerId + matchId` arrive concurrently while no projection row exists, both can observe no row, then both try to insert.
- One insert wins; the other gets `DataIntegrityViolationException`, which is converted to `InvalidMatchResultEventException`.
- `MatchResultConsumer` catches `InvalidMatchResultEventException` and acknowledges the Kafka record as non-retriable.

**Risk**
- A valid match-result event can be skipped permanently and not routed to retry/DLQ.
- This violates at-least-once/idempotent processing expectations for concurrent delivery.
- It also undercuts the out-of-order/same-match update behavior the service partially implements with `occurredAt` comparison.

**Required Fix**
- Make projection upsert race-safe.
- Options:
  - Lock an existing per-player/per-match row or deterministic parent row before projection mutation.
  - Use a MySQL upsert that applies the same `occurredAt` freshness rule atomically.
  - On duplicate-key conflict, reload the projection and re-apply the freshness rule instead of classifying as invalid.
- Add an integration or repository-backed test for two distinct events on the same player/match racing the initial projection insert.

### 2) HIGH - Stale `PROCESSING` requeue can duplicate live notification dispatch

**Where**
- `src/main/java/com/game/backend/game/service/NotificationWorker.java:65`
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:72`
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:79`
- `src/main/java/com/game/backend/game/service/NotificationWorker.java:81`
- `src/main/java/com/game/backend/game/service/NotificationWorker.java:88`

**Issue**
- A worker claims a job by setting it to `PROCESSING`, commits, then dispatches outside the transaction.
- Another worker periodically calls `requeueStaleProcessingJobs()` and turns any old `PROCESSING` row back into `RETRY_WAIT`.
- There is no worker lease token, heartbeat, fencing token, or conditional finalize that proves the original worker still owns the row.
- If dispatch takes longer than `processingTimeoutSeconds`, a second worker can requeue and dispatch the same job while the first dispatch is still running.
- The first worker can later call `markSent()` and overwrite whatever happened after the requeue.

**Risk**
- Duplicate notification delivery under slow provider calls, GC pauses, thread starvation, or long network timeouts.
- Final job state can hide the duplicate dispatch because stale ownership is not fenced.

**Required Fix**
- Add an ownership/lease model before requeueing `PROCESSING` jobs.
- At minimum, include a claim token/worker id or compare an expected `updatedAt`/version when finalizing.
- Finalization should be conditional on the row still being `PROCESSING` for the same claim.
- Add a test covering stale requeue racing with an in-flight dispatch.

### 3) MEDIUM - Stale `PROCESSING` retry path can exceed the configured max attempts

**Where**
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:72`
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:75`
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:79`
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:53`
- `src/main/java/com/game/backend/game/repository/NotificationJobRepository.java:56`

**Issue**
- `markProcessing()` increments `attemptCount`.
- `requeueStaleProcessingJobs()` moves all stale `PROCESSING` jobs to `RETRY_WAIT` regardless of `attemptCount`.
- A job that has already reached `maxAttempts` can be requeued, claimed again, and dispatched one more time before `applyDispatchFailure()` checks max attempts.
- If that extra dispatch succeeds, the job can be marked `SENT` after more than the configured maximum number of attempts.

**Risk**
- The notification retry contract says max attempts are `4` total.
- Crash/timeout paths can exceed that bound, which is exactly where bounded retry behavior matters most.

**Required Fix**
- Requeue stale processing jobs only when `attemptCount < maxAttempts`.
- Mark stale jobs with `attemptCount >= maxAttempts` as `FAILED_PERMANENT`.
- Add tests for stale `PROCESSING` rows at and below max attempts.

### 4) MEDIUM - Retry-exhausted transient DLQ behavior is no longer covered by an integration test

**Where**
- `src/test/java/com/game/backend/game/MatchResultConsumerIntegrationTest.java:150`
- `src/test/java/com/game/backend/game/service/MatchResultConsumerTest.java:104`
- `src/test/java/com/game/backend/game/service/MatchResultDlqPublisherTest.java:21`

**Issue**
- The old missing-player DLQ test was correctly changed to skip/no-DLQ behavior.
- No replacement integration test verifies an actual retryable transient failure is retried and routed to `match-results.v1.dlq` after exhaustion.
- Current tests cover listener rethrow without ack and DLQ payload construction in isolation, but not the configured `DefaultErrorHandler` retry/DLQ path.

**Risk**
- The core Phase 5 acceptance criteria require retry-exhausted failures to route to DLQ.
- Classifier mistakes, wrapped exception behavior, recoverer behavior, and wire serialization issues can pass current tests.

**Required Fix**
- Add a focused Spring/Kafka test that injects a retryable transient exception from the listener path and asserts:
  - retry attempts occur,
  - final record is published to `match-results.v1.dlq`,
  - DLQ payload is a JSON object with required metadata,
  - deterministic invalid events still skip without DLQ.

## Resolved Prior Findings

- Retry classification is no longer default retry-all; it now uses an explicit retryable allowlist.
- Concurrent duplicate delivery for the same `eventId` is improved via `INSERT IGNORE`.
- Missing player is treated as invalid/non-retriable instead of transient DLQ.
- DLQ no longer includes `originalPayload`.
- DLQ publishing now sends a structured object instead of a pre-serialized string.
- Notification dispatch no longer holds a pessimistic DB row lock during provider dispatch.
- Player id was removed from notification worker/dispatcher logs.
- Metric assertions were added for several consumer and worker paths.

## JavaDoc Compliance

- No JavaDoc compliance findings found for changed public Java classes/interfaces and primary public contract methods inspected.

## Report Artifact Notes

- `validation-report.md` is still absent and should be produced by the Validation Agent after implementation review fixes are complete.
- Prompt logging for this review was appended to `specs/2026-05-31-phase-5-remaining-event-processing/prompts.md`.

## Verdict

**Status: CHANGES_REQUIRED**

The Builder Agent fixed several prior issues, but the current implementation still has production-impact concurrency risks in match-result projection writes and notification worker stale requeue handling. The retry/DLQ acceptance path also needs an integration test with a real retryable failure.
