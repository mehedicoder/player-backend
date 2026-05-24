# Requirements - Phase 3 Authentication and Session Handling

## Scope

Included (from `specs/roadmap.md` Phase 3):
- Implement player login endpoint
- Implement session/token generation
- Store session data in Redis
- Add logout endpoint
- Add token/session validation filter
- Add rate limiting for login attempts
- Add security tests
- Add API tests for authentication flows

Excluded:
- Inventory/wallet/reward flows (Phase 4)
- Kafka async processing (Phase 5)
- Full production IAM/OAuth provider integration

## Decisions

1. Session strategy: opaque server-issued token, validated server-side via Redis.
2. Source of session truth: Redis.
3. Protected API model: explicit protected routes validated by auth filter.
4. Temporary credential model: local credential verification for Phase 3 scope.

## API Requirements

1. `POST /api/v1/auth/login`
   - Valid credentials: returns session token and expiry metadata.
   - Invalid credentials: returns auth error.

2. `POST /api/v1/auth/logout`
   - Requires valid token.
   - Revokes/deletes session in Redis.

3. Protected endpoints
   - Missing token: unauthorized.
   - Invalid/expired token: unauthorized.
   - Valid token: request proceeds.

### Auth Error and Status Contract

| Scenario | HTTP Status | Error Code |
| --- | --- | --- |
| Invalid login credentials | `401 Unauthorized` | `INVALID_CREDENTIALS` |
| Missing token on protected endpoint | `401 Unauthorized` | `AUTH_TOKEN_MISSING` |
| Invalid token on protected endpoint | `401 Unauthorized` | `AUTH_TOKEN_INVALID` |
| Expired token on protected endpoint | `401 Unauthorized` | `AUTH_TOKEN_EXPIRED` |
| Login rate limit exceeded | `429 Too Many Requests` | `AUTH_RATE_LIMITED` |

## Non-Functional Requirements

- Session TTL must be configurable.
- Login rate-limit threshold/window must be configurable.
- Error response shape must follow global error contract.
- Authentication behavior must be covered by unit + API tests.

## Artifact Placement Rule

Feature-specific artifacts must stay under:
- `specs/2026-05-23-phase-3-authentication-and-session-handling/`

Required files:
- `requirements.md`
- `plan.md`
- `validation.md`
