# Requirements - Player Activity Event Producer Foundation

## Scope

This slice defines the producer-side foundation for player activity events in Phase 5.

Included:
- Define Kafka topic contract: `player-activity-events.v1`
- Define event envelope contract (versioned)
- Define supported event types:
  - `wallet.credited.v1`
  - `wallet.debited.v1`
  - `inventory.mutated.v1`
  - `reward.claimed.v1`
- Implement producer abstraction and Kafka adapter in current service
- Publish events only after successful DB transaction commit for wallet/inventory/reward flows
- Add producer and contract tests for envelope shape, keys, and event-type mapping
- Define DLQ topic for producer-side failure routing contract (no consumer implementation in this slice)

Excluded:
- Kafka consumers (leaderboard, match result, analytics, notifications)
- Notification worker implementation
- Outbox pattern implementation
- Schema Registry integration
- New microservices or infrastructure components
- Kubernetes/deployment changes
- Replay/backfill tooling

### Event Envelope Contract (v1)

| Field | Type | Required | Notes |
|---|---|---|---|
| `eventId` | UUID/String | Yes | Unique event identifier for downstream dedupe |
| `eventType` | String | Yes | One of approved v1 event types |
| `schemaVersion` | String | Yes | Fixed to `v1` for this slice |
| `playerId` | String/Long | Yes | Also used as Kafka message key |
| `occurredAt` | ISO-8601 timestamp | Yes | Event occurrence time in UTC |
| `source` | String | Yes | Producing service identifier |
| `correlationId` | String | Optional | Request/workflow trace correlation |
| `idempotencyKey` | String | Optional | Pass-through from business operation where present |
| `payload` | Object | Yes | Event-specific non-sensitive data |

### Event Payload Contracts (v1)

All payloads must exclude sensitive data (credentials, tokens, secrets, personal contact data).

#### `wallet.credited.v1`
Required fields:
- `walletId` (String/Long)
- `transactionId` (String/UUID)
- `amount` (Number, positive)
- `currency` (String, ISO-style code used by service)
- `balanceAfter` (Number, non-negative)
- `reason` (String, business reason code)

Optional fields:
- `metadata` (Object, non-sensitive supplemental attributes)

#### `wallet.debited.v1`
Required fields:
- `walletId` (String/Long)
- `transactionId` (String/UUID)
- `amount` (Number, positive)
- `currency` (String, ISO-style code used by service)
- `balanceAfter` (Number, non-negative)
- `reason` (String, business reason code)

Optional fields:
- `metadata` (Object, non-sensitive supplemental attributes)

#### `inventory.mutated.v1`
Required fields:
- `itemId` (String/Long)
- `operation` (String, e.g. ADD/REMOVE/SET)
- `quantityDelta` (Integer)
- `quantityAfter` (Integer, non-negative)
- `mutationId` (String/UUID)

Optional fields:
- `metadata` (Object, non-sensitive supplemental attributes)

#### `reward.claimed.v1`
Required fields:
- `rewardId` (String/Long)
- `claimId` (String/UUID)
- `claimedAt` (ISO-8601 timestamp UTC)
- `rewardType` (String, domain-defined type)
- `grantSummary` (Object, non-sensitive reward grant summary)

Optional fields:
- `metadata` (Object, non-sensitive supplemental attributes)

## Decisions

Locked decisions for this slice:
- Topic name: `player-activity-events.v1`
- Event types:
  - `wallet.credited.v1`
  - `wallet.debited.v1`
  - `inventory.mutated.v1`
  - `reward.claimed.v1`
- Kafka message key: `playerId`
- Ordering guarantee: per-player ordering only
- Publish timing: publish only after successful DB commit
- Producer behavior: async publish; publish failures are logged and do not roll back already committed business transactions
- Idempotency model:
  - `eventId` is the dedupe handle for downstream consumers
  - existing business idempotency in wallet/reward logic remains unchanged
- DLQ scope:
  - DLQ topic name: `player-activity-events.v1.dlq`
  - DLQ consumer implementation deferred

## Context and Constraints

- Use current stack only (Java/Spring Boot/Maven/Kafka/MySQL/Redis/Testcontainers).
- No new microservices.
- No new infrastructure beyond existing Kafka.
- No Schema Registry in this slice.
- No outbox pattern unless separately approved by human architect.
- No sensitive data in event payloads.
- Existing APIs must remain backward compatible.
- Backfill/replay is out of scope.

## Artifact Placement Rule

Feature-specific files are stored under:
- `specs/2026-05-30-player-activity-event-producer-foundation/`

This includes:
- `prompts.md`
- `requirements.md`
- `plan.md`
- `validation.md`
- `spec-review-report.md` (to be authored by Reviewer Agent)
- `spec-validation-report.md` (to be authored by Validation Agent)
- `review-report.md` (to be authored by Reviewer Agent during implementation review)
- `validation-report.md` (to be authored by Validation Agent during implementation validation)
