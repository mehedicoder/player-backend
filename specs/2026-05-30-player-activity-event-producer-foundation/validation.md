# Validation - Player Activity Event Producer Foundation

## Automated Validation

Run:
1. `mvn -q -DskipTests compile`
2. `mvn -q -DskipTests=false test`

Required automated assertions:
- Envelope contract tests pass for required fields:
  - `eventId`, `eventType`, `schemaVersion`, `playerId`, `occurredAt`, `source`, `payload`
- Payload contract tests pass for each event type and required fields:
  - `wallet.credited.v1`: `walletId`, `transactionId`, `amount`, `currency`, `balanceAfter`, `reason`
  - `wallet.debited.v1`: `walletId`, `transactionId`, `amount`, `currency`, `balanceAfter`, `reason`
  - `inventory.mutated.v1`: `itemId`, `operation`, `quantityDelta`, `quantityAfter`, `mutationId`
  - `reward.claimed.v1`: `rewardId`, `claimId`, `claimedAt`, `rewardType`, `grantSummary`
  - Sensitive fields are excluded from payload contracts
- Event-type mapping tests pass:
  - wallet credit -> `wallet.credited.v1`
  - wallet debit -> `wallet.debited.v1`
  - inventory mutation -> `inventory.mutated.v1`
  - reward claim -> `reward.claimed.v1`
- Producer tests confirm:
  - topic `player-activity-events.v1`
  - DLQ topic constant `player-activity-events.v1.dlq`
  - key equals `playerId`
  - publish is async and failure path is logged
- Transactional behavior tests confirm:
  - publish hook runs only after successful commit
  - publish failure does not revert committed business mutation
- Existing wallet/inventory/reward tests continue to pass (no regression).

## Manual Validation

1. Execute representative wallet/inventory/reward success operations.
2. Verify corresponding event publication attempts are observable in logs.
3. Confirm envelope fields are populated correctly and contain no sensitive data.
4. Confirm no API contract changes are introduced to existing endpoints.
5. Verify no consumer process or worker was added in this slice.

## Edge Cases

- Missing/blank `correlationId` still permits publish with valid envelope.
- Missing `idempotencyKey` in operations that do not use idempotency still permits publish.
- Transient Kafka publish failure logs operationally useful context without failing client-facing business operation.
- High-frequency same-player operations maintain per-player keying (`playerId`).

## Definition of Done

- All included scope items in `requirements.md` are implemented.
- All excluded items remain out of scope.
- Test suite and contract checks pass.
- No sensitive event payload data is introduced.
- Backward compatibility of existing APIs is preserved.
