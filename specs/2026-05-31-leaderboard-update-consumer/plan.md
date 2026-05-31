# Plan - Leaderboard Update Consumer (Phase 5 Slice)

## 1. Data Model and Migration

1.1 Define leaderboard read-model schema (player score, updated timestamp, optional audit fields).  
1.2 Add/adjust processed-event dedupe table keyed by `eventId` (or equivalent durable dedupe structure).  
1.3 Add indexes for hot paths (`player_id`, score ordering, dedupe lookup).

## 2. Kafka Consumer Wiring

2.1 Add dedicated consumer group configuration for leaderboard updates.  
2.2 Register Kafka listener for `player-activity-events.v1`.  
2.3 Implement envelope validation and typed event dispatch.

## 3. Score Update Logic

3.1 Implement fixed v1 scoring matrix:
  - `wallet.credited.v1`: `+amount`
  - `wallet.debited.v1`: `-amount`
  - `reward.claimed.v1`: `+rewardAmount`
  - clamp resulting score to non-negative (`>= 0`)
  - `inventory.mutated.v1` excluded from scoring in this slice.  
3.2 Apply score mutation transactionally in MySQL.  
3.3 Record `eventId` as processed in same transaction boundary to enforce idempotency.

## 4. Error Handling and Retry

4.1 Add fixed retry policy for transient failures:
  - max attempts: `3`
  - exponential backoff: `500ms`, `1000ms`
  - retriable categories: transient DB/Kafka infrastructure exceptions.  
4.2 Route unsupported/invalid messages to explicit non-retry path:
  - commit offset
  - structured warning log (`eventId`, `eventType`, `playerId`, `reason`)
  - increment invalid/unsupported counter metric.  
4.3 Add structured operational logs for failures and retries.

## 5. Tests

5.1 Unit tests for event dispatch and score-delta mapping.  
5.2 Integration tests for successful consume-and-update flow.  
5.3 Integration tests for duplicate `eventId` handling (no double score mutation).  
5.4 Tests for invalid/unsupported event behavior.

## 6. Documentation and Task Tracking

6.1 Update `tasks.md` with implementation notes and test evidence.  
6.2 Keep roadmap status updates gated by validation evidence.  
6.3 Prepare reviewer/validator handoff docs in this feature folder.
