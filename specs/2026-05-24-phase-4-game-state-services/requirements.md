# Requirements - Phase 4 Game State Services

## Scope

Included (from `specs/roadmap.md` Phase 4):
- Implement Inventory Service
- Implement Wallet Service
- Add wallet ledger table
- Add idempotency key support for wallet operations
- Implement reward claim flow
- Publish player activity events
- Add transactional consistency for wallet updates
- Add tests for duplicate reward claims
- Add tests for concurrent wallet updates

Excluded:
- Authentication/session changes from Phase 3
- Kafka consumer retry/DLQ strategy from Phase 5
- Kubernetes/scale tuning from Phase 6
- CI/CD and production automation from Phase 7

## Decisions

1. Source of truth for wallet balance remains MySQL with append-only ledger entries.
2. Wallet mutations must be idempotent using client-provided idempotency key.
3. Reward claim is implemented as wallet credit + claim record with duplicate-claim protection.
4. Player activity events are produced asynchronously through Kafka after successful state mutation.
5. Redis is optional for read acceleration; Redis failure must not block durable wallet/inventory writes.

## API and Behavior Requirements

1. Inventory
- `GET /api/v1/players/{playerId}/inventory`
- `POST /api/v1/players/{playerId}/inventory/mutations`
- Behavior:
  - read returns current inventory snapshot.
  - mutation applies add/remove operation with validation.

2. Wallet
- `GET /api/v1/players/{playerId}/wallet`
- `POST /api/v1/players/{playerId}/wallet/mutations`
- Behavior:
  - read returns current wallet balance and metadata.
  - mutation appends ledger entry and returns updated balance.
  - duplicate idempotency key must return the originally persisted mutation result (`200`) and must not apply amount twice.

3. Reward claim
- `POST /api/v1/players/{playerId}/rewards/{rewardId}/claim`
- Required request field: `idempotencyKey`.
- Behavior:
  - first claim succeeds and applies exactly one credit.
  - duplicate claim for same player+reward must be idempotent and return `200` with prior claim result (no additional credit, no duplicate ledger entry).

4. Activity events
- Emit player activity event after successful inventory/wallet/reward mutation.
- Event topics:
  - `player.inventory.activity.v1`
  - `player.wallet.activity.v1`
  - `player.reward.activity.v1`

## Status and Error Contract

| Scenario | HTTP Status | Error Code |
| --- | --- | --- |
| Player not found | `404 Not Found` | `PLAYER_NOT_FOUND` |
| Invalid request payload/operation | `400 Bad Request` | `INVALID_REQUEST` |
| Insufficient wallet balance for debit | `409 Conflict` | `INSUFFICIENT_BALANCE` |
| Duplicate idempotency key (wallet mutation) | `200 OK` | N/A (returns original success payload) |
| Duplicate reward claim | `200 OK` | N/A (returns original success payload) |
| Unexpected infrastructure failure | `503 Service Unavailable` | `INFRA_UNAVAILABLE` |

## Non-Functional Requirements

- Wallet write path must be transactionally safe under concurrent requests.
- Concurrency correctness is mandatory for wallet updates and reward claims.
- SQL schema must include indexes for hot lookup paths (wallet by player, ledger by player/time, idempotency key lookup).
- Tests must cover duplicate requests, race conditions, and normal success paths.
