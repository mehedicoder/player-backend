# Validation - Phase 4 Game State Services

## Automated

1. `mvn -q -DskipTests compile` must pass.  
2. `mvn -q -DskipTests=false test` must pass.  
3. Integration test set must validate:
- wallet ledger persistence correctness
- idempotency key duplicate handling
- duplicate reward claim protection
- concurrent wallet update correctness
- player activity event publication on successful mutations, including:
  - `player.inventory.activity.v1`
  - `player.wallet.activity.v1`
  - `player.reward.activity.v1`

## Manual

1. Start dependencies:
- `docker compose up -d`
2. Start app:
- `mvn -Dspring-boot.run.profiles=local spring-boot:run`
3. Wallet mutation happy path:
- apply credit/debit and verify ledger row + resulting balance.
4. Idempotency check:
- repeat same wallet mutation with same idempotency key and verify no double-application.
5. Reward claim check:
- claim reward once (success), then claim again with same idempotency semantics and verify:
  - second response is `200` with same claim outcome
  - no second credit in wallet
  - no duplicate claim row and no duplicate ledger row
6. Concurrency check:
- send parallel wallet mutations and verify final balance and ledger count are consistent.
7. Activity event check:
- verify event publication using one of:
  - integration test assertion against producer mock/spies, or
  - Kafka topic consumption check for:
    - `player.inventory.activity.v1`
    - `player.wallet.activity.v1`
    - `player.reward.activity.v1`
  - and assert event count equals successful state transitions only.

## Definition of Done

- Phase 4 roadmap tasks in scope are implemented.
- Wallet operations are ledger-backed and transactionally safe.
- Idempotency and duplicate-claim protections are validated.
- Concurrency validation demonstrates no balance corruption.
- Activity events are emitted for successful relevant operations.
