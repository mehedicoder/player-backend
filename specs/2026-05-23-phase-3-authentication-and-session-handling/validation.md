# Validation - Phase 3 Authentication and Session Handling

## Automated

1. `mvn -q -DskipTests compile` must pass.  
2. `mvn -q -DskipTests=false test` must pass.  
3. Authentication API tests must cover:
   - login success
   - invalid credentials
   - missing token on protected API
   - invalid/expired token on protected API
   - logout invalidates session token
   - login rate limit exceeded path

## Manual

1. Start dependencies:
   - `docker compose up -d`
2. Start app with local profile:
   - `mvn "-Dspring-boot.run.profiles=local" spring-boot:run`
3. Login flow:
   - call `POST /api/v1/auth/login` with valid credentials and verify token response
4. Protected endpoint authorization:
   - valid token -> success
   - missing token -> unauthorized
   - invalid/expired token -> unauthorized
5. Logout flow:
   - call `POST /api/v1/auth/logout`, then verify token can no longer access protected APIs
6. Brute-force protection:
   - repeat invalid logins and verify throttling response is returned

## Definition of Done

- All Phase 3 tasks in `specs/roadmap.md` are implemented.
- Login, logout, token validation filter, and Redis session storage are validated.
- Rate-limiting behavior is validated.
- Automated tests and manual checks provide evidence for all Phase 3 acceptance criteria.
