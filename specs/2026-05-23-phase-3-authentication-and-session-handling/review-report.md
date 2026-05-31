# Review Report - Phase 3 Authentication and Session Handling

Feature: `specs/2026-05-23-phase-3-authentication-and-session-handling`  
Date: 2026-05-23  
Role: Reviewer Agent

## Findings

### 1) HIGH - Invalid token path returns `AUTH_TOKEN_EXPIRED` instead of `AUTH_TOKEN_INVALID`

- File: `src/main/java/com/game/backend/auth/service/AuthService.java`
- Issue:
  - `validateToken()` maps missing session state (`sessionStore.get(token)` empty) to `AuthTokenExpiredException`.
  - This means a random/unknown token is reported as expired.
- Contract mismatch:
  - Phase 3 requirements define:
    - invalid token -> `AUTH_TOKEN_INVALID`
    - expired token -> `AUTH_TOKEN_EXPIRED`
- Impact:
  - API error semantics are inaccurate and can break client behavior that relies on error code type.
- Recommendation:
  - Treat "token not found in store" as `AuthTokenInvalidException`.
  - Reserve `AuthTokenExpiredException` for known token whose `expiresAt` is in the past.

### 2) MEDIUM - Logout endpoint does not enforce token validity despite spec requirement

- Files:
  - `src/main/java/com/game/backend/auth/api/AuthController.java`
  - `src/main/java/com/game/backend/auth/service/AuthService.java`
- Issue:
  - `logout()` only parses header and deletes the token key.
  - Unknown/expired tokens still return success response.
- Contract mismatch:
  - Phase 3 spec says logout "Requires valid token".
- Impact:
  - Auth contract is weaker than specified and hides invalid-token usage.
- Recommendation:
  - Validate token in logout path (reuse `validateToken()` semantics), then revoke.
  - Return unauthorized error code for invalid/expired token.

## Open Questions / Assumptions

- Assumption: current protected-route scope (`GET`/`PUT` player profile endpoints) is intentional for this phase.

## Summary

Implementation is close, but two auth-contract mismatches should be fixed before validator signoff.
