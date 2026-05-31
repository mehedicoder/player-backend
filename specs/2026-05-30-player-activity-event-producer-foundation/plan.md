# Implementation Plan - Player Activity Event Producer Foundation

## 1. Event Contract Definition
1. Create internal event envelope model for `schemaVersion = v1`.
2. Define and centralize allowed event type constants:
   - `wallet.credited.v1`
   - `wallet.debited.v1`
   - `inventory.mutated.v1`
   - `reward.claimed.v1`
3. Define producer topic constants:
   - primary topic: `player-activity-events.v1`
   - DLQ topic name constant: `player-activity-events.v1.dlq` (contract only; no consumer implementation)
4. Define payload field guidelines to prevent sensitive-data leakage.
5. Define per-event payload schemas for:
   - `wallet.credited.v1`
   - `wallet.debited.v1`
   - `inventory.mutated.v1`
   - `reward.claimed.v1`

## 2. Producer Abstraction and Kafka Adapter
1. Create/confirm producer interface owned by game activity domain boundary.
2. Implement Kafka adapter using existing Spring Kafka setup.
3. Serialize envelope + payload with existing project serialization patterns.
4. Use `playerId` as Kafka message key in all publish calls.
5. Enforce async publish with structured failure logging.

## 3. Transaction-After-Commit Publication Integration
1. Integrate wallet success paths to publish `wallet.credited.v1` and `wallet.debited.v1`.
2. Integrate inventory success paths to publish `inventory.mutated.v1`.
3. Integrate reward claim success paths to publish `reward.claimed.v1`.
4. Ensure publish is invoked only after successful DB commit.
5. Confirm failure to publish does not roll back committed business transaction.

## 4. Tests
1. Add contract tests verifying envelope shape and required fields.
2. Add contract tests verifying per-event payload required/optional fields and sensitive-field exclusions.
3. Add mapping tests validating each business action maps to expected `eventType`.
4. Add producer tests validating topic and key usage (`playerId`) and configured DLQ topic constant.
5. Add behavior tests validating async-failure logging and no business rollback coupling.
6. Run existing relevant unit/integration test suites for wallet/inventory/reward regression coverage.

## 5. Documentation and Readiness
1. Document event contract fields and event types in implementation docs/comments where appropriate.
2. Document explicit out-of-scope items (consumers, outbox, schema registry, replay/backfill).
3. Prepare handoff notes for next Phase 5 slice (consumer foundation, retries, DLQ handling).
