# Spec Review Report - 2026-05-31-phase-5-remaining-event-processing

## Review Scope

- `requirements.md`
- `plan.md`
- `validation.md`

## Findings

### 1) HIGH - Match-result contract is ambiguous on player identity model

**Where**
- `requirements.md` (Functional Requirements > Match Result Consumer)

**Issue**
- The spec allows `playerId` **or** participating player list, but does not define which one is required in v1.
- This creates incompatible implementation paths for storage model, dedupe keys, and notification fan-out behavior.

**Risk**
- Builder and validator can implement different assumptions and both appear locally valid.
- Review/validation will be blocked by contract mismatch.

**Required Fix**
- Lock the v1 contract to one model:
  - either single-player result event with required `playerId`, or
  - multi-participant event with required `participants[]` schema and deterministic per-player update rules.
- Add exact required payload fields and failure behavior for missing participant/player fields.

### 2) HIGH - Retry policy is underspecified (attempts/backoff/exception class boundaries)

**Where**
- `requirements.md` (Retry + DLQ)
- `validation.md` (Retry assertions)

**Issue**
- Spec says "bounded retry" but does not set concrete values for max attempts/backoff progression.
- "Transient infrastructure/data access failures" is too broad without explicit exception-category boundaries.

**Risk**
- Non-deterministic runtime behavior across environments and inconsistent validator expectations.

**Required Fix**
- Define exact retry contract in spec:
  - max attempts (initial + retries)
  - backoff schedule (fixed or exponential with exact values)
  - explicit retriable exception categories
  - explicit non-retriable categories.

### 3) MEDIUM - DLQ contract lacks concrete topic naming and payload envelope requirements

**Where**
- `requirements.md` (Retry + DLQ)
- `plan.md` (4.3)

**Issue**
- DLQ existence is specified, but not the concrete topic naming convention or required fields forwarded to DLQ records.

**Risk**
- Inconsistent naming and payload shape, reducing operability and replayability.

**Required Fix**
- Define v1 DLQ topic name convention (or explicit topic).
- Define minimal DLQ record fields (`originalTopic`, `partition`, `offset`, `eventId`, `eventType`, `consumerGroup`, `errorCategory`, `errorMessage`, `failedAt`).

### 4) MEDIUM - Notification worker acceptance behavior is incomplete

**Where**
- `requirements.md` (Notification Worker)
- `validation.md` (Required Assertions / Manual Validation)

**Issue**
- Spec says async notification work should be created and dispatch failures should be observable, but does not define:
  - success criterion (stored as durable job vs fire-and-forget publish)
  - retry policy for notification dispatch path
  - terminal failure state handling.

**Risk**
- Worker may be implemented without durable recovery semantics, undermining reliability.

**Required Fix**
- Define notification work model for v1 (durable job table or durable event handoff).
- Define retry/terminal state semantics for worker dispatch.
- Add validation checks for retry-exhausted notification jobs and observability signals.

## Spec Gaps Blocking Builder/Validator

- Finding #1 (HIGH): ambiguous player identity model in match-result contract.
- Finding #2 (HIGH): missing concrete retry policy contract.

## Positives

- Required artifacts are present (`requirements.md`, `plan.md`, `validation.md`).
- Scope boundaries are mostly clear and aligned with remaining Phase 5 roadmap task group.
- Validation includes both automated and manual paths and calls out regression safety.

## Review Verdict

- **Status: CHANGES_REQUIRED**
- Builder should address the blocking gaps first, then reviewer re-check is recommended before validation phase.
