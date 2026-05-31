# Validation - Leaderboard Update Consumer (Phase 5 Slice)

## Automated Validation

1. Build/compile:
   - `mvn -q -DskipTests compile`
2. Targeted tests:
   - Consumer success-path tests
   - Duplicate event idempotency tests
   - Invalid/unsupported event tests
3. Full regression check (if scope impact requires):
   - `mvn -q -DskipTests=false test`

## Required Assertions

- Supported events are consumed and update score with fixed formulas:
  - `wallet.credited.v1` -> `+amount`
  - `wallet.debited.v1` -> `-amount`
  - `reward.claimed.v1` -> `+rewardAmount`
- Score never becomes negative (v1 clamp-to-zero rule).
- Duplicate delivery of same `eventId` does not mutate score twice.
- Invalid envelope or unsupported event type:
  - is treated as non-retriable
  - commits offset (skip semantics)
  - emits structured warning log and invalid/unsupported counter signal
  - causes no state mutation and no crash loop.
- Retry policy is applied exactly as specified:
  - max attempts `3` (initial + 2 retries)
  - exponential backoff `500ms`, `1000ms`
  - only transient DB/Kafka infrastructure failures are retried.

## Manual Validation

1. Start local dependencies (`docker compose up -d`) and app (`local` profile).  
2. Produce representative player-activity events on topic `player-activity-events.v1`.  
3. Verify leaderboard table/read model updates per expected score rules.  
4. Re-send same event (`eventId` unchanged) and confirm no duplicate score update.  
5. Send unsupported/invalid event and confirm:
   - no score update
   - offset is advanced/committed
   - warning log + counter signal present.
6. Inject transient DB/Kafka failure and confirm retry attempt count/backoff behavior.

## Edge Cases

- Out-of-order events for same player.
- Null/missing required envelope fields.
- Consumer restart during processing.
- Transient DB/Kafka interruptions during consume flow.
- Score-underflow scenario (debit exceeds current score) clamps to `0`.

## Definition of Done

- Consumer implementation merged with tests and passing validation.
- Idempotent processing by `eventId` demonstrated with evidence.
- Invalid/unsupported input skip+commit behavior documented and verified.
- Retry policy evidence shows configured attempts/backoff and category boundaries.
- No out-of-scope scope creep introduced (WebSocket/season/regional/DLQ replay/backfill/etc.).
