# Spec Validation Report - 2026-05-31-phase-5-remaining-event-processing

## Verdict

**CHANGES_REQUIRED**

The feature-spec is close to execution-ready and addresses prior review blockers, but still has a couple of medium-severity spec-quality gaps that can cause inconsistent builder/validator execution.

## Findings

### 1) MEDIUM - Automated validation commands are not fully concrete for targeted scope execution
**File:** `specs/2026-05-31-phase-5-remaining-event-processing/validation.md`

**Evidence**
- Under "Automated Validation", item 2 says "Targeted tests for this slice" but does not provide exact runnable Maven command(s) or test-class patterns.

**Why this matters**
- Different agents may run different subsets, making validation evidence non-comparable.

### 2) MEDIUM - Plan includes conditional phrasing that weakens execution determinism
**File:** `specs/2026-05-31-phase-5-remaining-event-processing/plan.md`

**Evidence**
- Section 1.2 includes: "match result projection/state (if required by existing domain model)".

**Why this matters**
- "If required" leaves implementation branching unspecified and can lead to scope drift or rework during build.

### 3) LOW - Manual validation step for app startup is underspecified
**File:** `specs/2026-05-31-phase-5-remaining-event-processing/validation.md`

**Evidence**
- Manual section says "Start app with local profile" without exact command and observable success signal.

**Why this matters**
- Minor repeatability issue for handoff and validator consistency.

## Spec Validation Gaps Blocking Builder/Validator

**None (no blockers).**

- Required artifacts exist: `requirements.md`, `plan.md`, `validation.md`.
- Prior spec-review HIGH blockers are resolved:
  - v1 player identity model is now explicit (single-player with required `playerId`).
  - retry contract is now concrete (attempts, backoff, categories).
- Traceability to Phase 5 remaining tasks is present and coherent across requirements/plan/validation.

## Concrete Remediation Actions

1. In `validation.md`, replace "Targeted tests for this slice" with exact command(s), for example:
   - `mvn -q -DskipTests=false "-Dtest=MatchResult*Test,Notification*Test,*Dlq*Test" test`
   - or explicit known class list once names are finalized.

2. In `plan.md` section 1.2, remove conditional phrasing and choose one explicit v1 path:
   - either always persist a match-result projection table,
   - or explicitly defer projection and document exact persistence boundary for v1.

3. In `validation.md` manual section, add concrete startup and health evidence command(s), for example:
   - `mvn "-Dspring-boot.run.profiles=local" spring-boot:run`
   - `curl http://localhost:8080/actuator/health` (expect HTTP 200, `status=UP`).

## Validation Summary

The spec is structurally complete and mostly execution-ready. After the medium gaps are tightened, it should be ready for implementation with consistent validation evidence.
