# Plan - Phase 2 Core Player Platform

## 1. Data Model and Migration

1.1 Define initial player table schema (ID, profile fields, timestamps, versioning fields if needed).  
1.2 Add Flyway migration for player schema creation.  
1.3 Ensure migration ordering and naming are consistent with existing Flyway setup.

## 2. API and Application Layer

2.1 Implement player create/seed API (approved extension).  
2.2 Implement Player Profile Read API.  
2.3 Implement Player Profile Update API.  
2.4 Add request DTO validation for create/update requests.  
2.5 Add global exception handling and clear API error contracts.

## 3. Data Access and Caching

3.1 Implement repository access for player persistence in MySQL.  
3.2 Apply DB-first read behavior with optional minimal Redis cache-aside for repeated reads.  
3.3 Implement Redis fallback behavior so profile APIs still work when Redis is unavailable.

## 4. API Documentation

4.1 Add OpenAPI annotations/contracts for create, read, and update endpoints.  
4.2 Ensure request/response examples and error responses are documented.

## 5. Test Coverage

5.1 Add unit tests for service and validation logic.  
5.2 Add integration tests for MySQL-backed create/read/update flow.  
5.3 Add integration tests for Redis available/unavailable behavior.  
5.4 Add negative-path tests: player not found, invalid player ID, invalid update payload.

## 6. Acceptance Validation

6.1 Player can be created or seeded in MySQL.  
6.2 Player profile read endpoint returns persisted profile.  
6.3 Player profile update endpoint persists and returns updated state.  
6.4 App behavior remains correct when Redis is unavailable.  
6.5 OpenAPI docs are available and reflect implemented endpoints.
