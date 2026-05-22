# Roadmap

## Task Status Legend

- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS] Currently being worked on
- [ ] [BLOCKED] Blocked by dependency, missing decision, or environment issue

---

## Phase 1: Local Development Environment

Goal: Create a repeatable local development setup so every agent and developer can run the backend consistently on one machine.

Tasks:
- [ ] Verify Docker Engine or Docker Desktop is installed
- [ ] Add `docker-compose.yml` for local infrastructure
- [ ] Add MySQL container
- [ ] Add Redis container
- [ ] Add optional Kafka container for later phases
- [ ] Add persistent Docker volumes for MySQL and Redis
- [ ] Add health checks for MySQL and Redis containers
- [ ] Add `application-local.yml` for local Spring Boot configuration
- [ ] Configure Spring Boot connection to MySQL
- [ ] Configure Spring Boot connection to Redis
- [ ] Add Flyway or Liquibase support for database migrations
- [ ] Add Spring Boot Actuator health endpoint
- [ ] Document local setup in `docs/local-development.md`

Acceptance Criteria:
- [ ] `docker compose up -d` starts local infrastructure
- [ ] MySQL is available on `localhost:3306`
- [ ] Redis is available on `localhost:6379`
- [ ] Spring Boot application starts with the `local` profile
- [ ] Application can connect to MySQL
- [ ] Application can connect to Redis
- [ ] `/actuator/health` returns healthy status
- [ ] Local setup instructions are documented

---

## Phase 2: Core Player Platform

Goal: Build the first vertical slice of the player backend using MySQL as the source of truth and Redis for hot read caching.

Tasks:
- [ ] Create initial player database schema
- [ ] Add Flyway migration for player tables
- [ ] Implement Player Profile Read API
- [ ] Implement Player Profile Update API
- [ ] Add Redis cache-aside strategy for hot player profile reads
- [ ] Add Redis fallback behavior if Redis is unavailable
- [ ] Add request validation
- [ ] Add global exception handling
- [ ] Add unit tests
- [ ] Add integration tests for MySQL and Redis
- [ ] Add API documentation using OpenAPI

Acceptance Criteria:
- [ ] Player profile can be created or seeded in MySQL
- [ ] Player profile can be read through REST API
- [ ] Player profile can be updated through REST API
- [ ] Redis cache is used for repeated player profile reads
- [ ] Application still works if Redis is temporarily unavailable
- [ ] Tests cover cache hit, cache miss, player not found, invalid player ID, and update flow
- [ ] OpenAPI documentation is available

---

## Phase 3: Authentication and Session Handling

Goal: Add secure player authentication and session management.

Tasks:
- [ ] Implement player login endpoint
- [ ] Implement session/token generation
- [ ] Store session data in Redis
- [ ] Add logout endpoint
- [ ] Add token/session validation filter
- [ ] Add rate limiting for login attempts
- [ ] Add security tests
- [ ] Add API tests for authentication flows

Acceptance Criteria:
- [ ] Player can log in successfully
- [ ] Invalid credentials are rejected
- [ ] Session/token is validated for protected APIs
- [ ] Player can log out
- [ ] Login brute-force protection exists
- [ ] Authentication tests pass

---

## Phase 4: Game State Services

Goal: Add core game state functionality for inventory, wallet, rewards, and activity tracking.

Tasks:
- [ ] Implement Inventory Service
- [ ] Implement Wallet Service
- [ ] Add wallet ledger table
- [ ] Add idempotency key support for wallet operations
- [ ] Implement reward claim flow
- [ ] Publish player activity events
- [ ] Add transactional consistency for wallet updates
- [ ] Add tests for duplicate reward claims
- [ ] Add tests for concurrent wallet updates

Acceptance Criteria:
- [ ] Inventory can be read and updated
- [ ] Wallet balance is updated through a ledger model
- [ ] Duplicate wallet or reward requests are safely ignored
- [ ] Concurrent wallet updates do not corrupt balance
- [ ] Player activity events are produced
- [ ] Tests cover normal and failure scenarios

---

## Phase 5: Event-Driven Processing

Goal: Use Kafka to decouple asynchronous workflows.

Tasks:
- [ ] Add Kafka producer for player events
- [ ] Add Kafka producer for wallet events
- [ ] Add Kafka consumer for leaderboard updates
- [ ] Add Kafka consumer for match result ingestion
- [ ] Add async notification worker
- [ ] Add retry handling
- [ ] Add dead-letter topic strategy
- [ ] Add Kafka integration tests

Acceptance Criteria:
- [ ] Player events are published to Kafka
- [ ] Consumers process events idempotently
- [ ] Failed events are retried
- [ ] Unrecoverable events are routed to dead-letter topics
- [ ] Kafka consumer lag can be monitored
- [ ] Kafka integration tests pass

---

## Phase 6: Scale and Reliability

Goal: Prepare the backend for high-throughput, low-latency workloads.

Tasks:
- [ ] Add Kubernetes deployment manifests
- [ ] Add readiness and liveness probes
- [ ] Add Horizontal Pod Autoscaler configuration
- [ ] Add resource requests and limits
- [ ] Add MySQL indexing strategy
- [ ] Add MySQL read replica strategy
- [ ] Add Redis timeout and fallback configuration
- [ ] Add load testing scripts
- [ ] Add JVM memory and GC tuning notes
- [ ] Add observability dashboards

Acceptance Criteria:
- [ ] Application can run in Kubernetes
- [ ] Pods expose readiness and liveness checks
- [ ] Application can scale horizontally
- [ ] Database indexes support hot queries
- [ ] Redis failures do not break critical APIs
- [ ] Load test results are documented
- [ ] JVM and GC metrics are observable

---

## Phase 7: Production Readiness

Goal: Make the system ready for secure, observable, automated deployment.

Tasks:
- [ ] Add CI/CD pipeline
- [ ] Add Terraform infrastructure definitions
- [ ] Add ArgoCD deployment configuration
- [ ] Add security review checklist
- [ ] Add API rate limiting
- [ ] Add structured logging
- [ ] Add alerting rules
- [ ] Add operational runbook
- [ ] Add rollback strategy
- [ ] Add production readiness checklist

Acceptance Criteria:
- [ ] CI pipeline runs tests and quality checks
- [ ] Infrastructure is defined with Terraform
- [ ] Deployment is managed through ArgoCD
- [ ] Alerts exist for latency, error rate, Redis failures, MySQL failures, and Kafka lag
- [ ] Runbook documents common incidents
- [ ] Rollback procedure is documented
- [ ] Security review is completed