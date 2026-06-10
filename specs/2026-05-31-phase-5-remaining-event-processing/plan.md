# Plan - Phase 5 Remaining Event Processing Slice

## 1. Contract and Data Foundations

1.1 Lock v1 match-result topic and event contract for single-player ingestion:
- topic: `match-results.v1`
- event type: `match.result.recorded.v1`
- envelope required: `eventId`, `eventType`, `playerId`, `occurredAt`, `payload`
- payload required: `matchId`, `result` (`WIN|LOSS|DRAW`), `scoreDelta` (`-1000..1000`).  
1.2 Add/extend MySQL tables for:
- processed-event dedupe (if reuse is not sufficient)
- match result projection/state table for v1 consumer-applied match outcome data
- notification job tracking (durable queue semantics: status, attempt count, next-at, last-error).  
1.3 Add indexes for hot lookups (`event_id`, player/match keys, job status where relevant).

## 2. Match Result Consumer

2.1 Configure dedicated consumer group/container.  
2.2 Implement envelope parse + validation + supported-event dispatch (`match.result.recorded.v1`).  
2.3 Apply idempotent transactional processing:
- dedupe check
- state mutation
- dedupe mark persisted in same transaction.

## 3. Async Notification Worker

3.1 Implement durable notification job record model in MySQL with status transitions:
- `PENDING`, `PROCESSING`, `RETRY_WAIT`, `SENT`, `FAILED_PERMANENT`.  
3.2 Create notification job atomically with successful match-result processing transaction.  
3.3 Implement worker execution with bounded concurrency and retry policy:
- max attempts `4` (initial + 3 retries)
- backoff `1s`, `2s`, `4s`
- terminal state `FAILED_PERMANENT` on exhaustion.

## 4. Retry and DLQ Strategy

4.1 Configure match-result consumer retry policy:
- max attempts `3` (initial + 2 retries)
- backoff `500ms`, `1000ms`
- retriable exceptions limited to transient DB/Kafka/transaction-system categories.  
4.2 Configure non-retriable skip/commit path for invalid/unsupported input.  
4.3 Configure retry-exhausted path to publish/route to `match-results.v1.dlq` with standard DLQ payload metadata fields.  
4.4 Ensure no poison-message infinite loop.

## 5. Tests

5.1 Unit tests:
- event validation/dispatch
- idempotency duplicate handling
- retry/non-retry classification
- DLQ routing behavior.  
5.2 Integration tests (embedded Kafka + DB):
- success ingestion path
- duplicate event replay path
- transient failure retry path
- retry exhausted to DLQ path
- async notification job creation/processing path
- notification retry and terminal `FAILED_PERMANENT` path.

## 6. Documentation and Tracking

6.1 Update `tasks.md` with implementation evidence and commands executed.  
6.2 Keep roadmap markers unchanged until validation evidence confirms completion.  
6.3 Prepare review and validation report placeholders in this feature folder after implementation.
