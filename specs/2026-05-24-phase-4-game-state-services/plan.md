# Plan - Phase 4 Game State Services

## 1. Data Model and Migrations

1.1 Add wallet ledger table for immutable credit/debit records.  
1.2 Add idempotency table (or idempotency columns) for deduplication checks.  
1.3 Add reward-claim persistence model with uniqueness constraints.  
1.4 Add indexes for player-centric reads and idempotency-key lookups.

## 2. Inventory Service

2.1 Implement inventory read endpoint/service.  
2.2 Implement inventory mutation endpoint/service with input validation.  
2.3 Define error handling for not found/invalid operation/insufficient quantity.

## 3. Wallet Service

3.1 Implement wallet balance read endpoint/service.  
3.2 Implement wallet mutation endpoint using ledger append model.  
3.3 Enforce idempotency key checks before applying mutation.  
3.4 Ensure transactional boundaries protect against partial updates.

## 4. Reward Claim Flow

4.1 Implement reward claim endpoint/service.  
4.2 Enforce duplicate claim protection with deterministic response behavior.  
4.3 Apply wallet credit and claim record atomically.

## 5. Player Activity Event Publishing

5.1 Define event payload schema for inventory/wallet/reward activity.  
5.2 Publish event after successful transaction commit.  
5.3 Add failure handling contract (log/metrics/retry strategy aligned with approved infra scope).

## 6. Test Coverage

6.1 Unit tests for business rules and validation.  
6.2 Integration tests for transactional wallet updates.  
6.3 Concurrency tests for parallel wallet mutation requests.  
6.4 Duplicate reward claim and duplicate idempotency key tests.  
6.5 Event publication contract tests for successful mutation paths.
