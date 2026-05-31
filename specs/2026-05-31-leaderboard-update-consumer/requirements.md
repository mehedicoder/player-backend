# Requirements - Leaderboard Update Consumer (Phase 5 Slice)

## Scope

This v1 slice introduces a Kafka consumer that updates a leaderboard read model from selected player-activity events.

### In Scope

- Kafka consumer for player activity topic (`player-activity-events.v1`)
- Consume only event types relevant to leaderboard scoring:
  - `wallet.credited.v1`
  - `wallet.debited.v1`
  - `reward.claimed.v1`
- Update a MySQL leaderboard score table/read model
- Idempotent event processing using `eventId`
- Basic retry/error handling for transient failures with fixed policy:
  - max attempts: `3` (initial + 2 retries)
  - backoff: exponential (`500ms`, `1000ms`)
  - retriable failures: transient DB exceptions and transient Kafka/client infrastructure exceptions
  - non-retriable failures: malformed envelope, unsupported event type, missing required payload fields
- Tests for:
  - successful consumption and score update
  - duplicate event handling
  - invalid/unsupported event handling

### Out of Scope

- Real-time WebSocket leaderboard updates
- Complex ranking algorithms
- Multiple leaderboard seasons
- Global/regional leaderboard separation
- DLQ replay tooling
- Backfill from historical events
- Separate leaderboard microservice
- Redis sorted-set leaderboard
- Admin leaderboard management UI
- `inventory.mutated.v1` scoring in v1 (deferred to follow-up slice)

### Data/Contract Notes

| Field | Source | Usage |
|---|---|---|
| `eventId` | activity envelope | consumer dedupe key |
| `eventType` | activity envelope | dispatch scoring logic |
| `playerId` | activity envelope | leaderboard player key |
| `occurredAt` | activity envelope | ordering/audit metadata |
| `payload` | activity envelope | score delta inputs |

### V1 Scoring Matrix (Deterministic Contract)

| Event Type | Required Payload Fields | Score Delta Formula | Missing/Invalid Required Fields |
|---|---|---|---|
| `wallet.credited.v1` | `amount` (positive number) | `+amount` | non-retriable; log warning with `eventId`, commit offset, do not update score |
| `wallet.debited.v1` | `amount` (positive number) | `-amount` | non-retriable; log warning with `eventId`, commit offset, do not update score |
| `reward.claimed.v1` | `rewardAmount` (positive number) | `+rewardAmount` | non-retriable; log warning with `eventId`, commit offset, do not update score |

Notes:
- `inventory.mutated.v1` is not scored in this v1 slice.
- If computed score would go below `0`, clamp to `0` in v1 to keep leaderboard score non-negative.
- Duplicate `eventId` must be treated as already processed (no second score update).

### Unsupported/Invalid Event Handling Contract

- Unsupported `eventType` or invalid envelope/payload:
  - treated as non-retriable
  - offset is committed (skip semantics to avoid poison-message loops)
  - structured warning log required with: `eventId`, `eventType`, `playerId`, `reason`
  - increment invalid/unsupported event counter metric (or equivalent observable counter)

## Decisions

1. Persistence: **MySQL leaderboard table/read model**.
2. Consumer isolation: **dedicated Kafka consumer group** for leaderboard updates.
3. Delivery semantics: Kafka at-least-once with **idempotent apply via `eventId`**.
4. Feature size: **minimal shippable vertical slice** with narrow event support and explicit exclusions.
5. Offset/error policy:
   - successful processing and duplicates: commit offset
   - invalid/unsupported events: commit offset without retry
   - retriable transient failures: retry up to max attempts; if exhausted, fail processing and leave offset uncommitted for redelivery

Rationale:
- Matches current stack and existing durability model.
- Keeps consumer behavior deterministic under duplicate delivery.
- Limits blast radius while enabling end-to-end async score updates.

## Context

- Follow existing Spring Boot + Kafka + MySQL patterns in repo.
- No new infrastructure/services/frameworks.
- Keep scoring logic simple and explicit for v1.
- Prefer concise, observable failure handling (structured logs + retry path).

## Artifact Placement Rule

All feature-specific files for this slice are stored under:

- `specs/2026-05-31-leaderboard-update-consumer/`

Expected report artifacts for this folder:

- `spec-review-report.md`
- `spec-validation-report.md`
- `review-report.md`
- `validation-report.md`
