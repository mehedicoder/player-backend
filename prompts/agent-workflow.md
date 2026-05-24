# Agent Workflow

This project uses a role-based multi-agent workflow.

The agents are separated by responsibility, not by machine. All agents may work on the same feature branch, but each agent has a different purpose, different rules, and different output files.

There is no Architect Agent. The human engineer is the architect.

---

## Roles

The workflow uses these roles:

1. Human Architect
2. Builder Agent
3. Reviewer Agent
4. Validation Agent

---

## Human Architect

The Human Architect owns all architecture and production decisions.

The Human Architect decides:

- System architecture
- Service boundaries
- Database design
- Redis usage
- Kafka usage
- Consistency model
- Security boundaries
- Infrastructure strategy
- Production trade-offs
- Deployment strategy
- Final approval
- Commit, push, merge, and deployment decisions

Agents may suggest improvements, risks, or alternatives, but they do not own architecture decisions.

---

## Project Context Files

Before working, agents should read the relevant project documents:

- `specs/mission.md`
- `specs/tech-stack.md`
- `specs/roadmap.md`
- `specs/architecture.md` if available
- `specs/api-contract.md` if available
- `specs/database-design.md` if available
- `specs/local-development.md` if available
- `tasks.md` if available
- Active feature spec review report in `specs/YYYY-MM-DD-<feature-name>/spec-review-report.md` if available
- Active feature spec validation report in `specs/YYYY-MM-DD-<feature-name>/spec-validation-report.md` if available
- Active feature review report in `specs/YYYY-MM-DD-<feature-name>/review-report.md` if available
- Active feature validation report in `specs/YYYY-MM-DD-<feature-name>/validation-report.md` if available

---

## Builder Agent

The Builder Agent implements human-approved roadmap tasks.

Main outputs:

- Source code
- Tests
- Configuration files
- Database migrations
- Documentation updates
- `tasks.md`

The Builder Agent must follow the human-approved architecture and must not make major architecture changes without explicit human approval.

---

## Reviewer Agent

The Reviewer Agent reviews the implementation like a senior backend engineer and production owner.

Main output:

- Spec review: `specs/YYYY-MM-DD-<feature-name>/spec-review-report.md`
- Implementation review: `specs/YYYY-MM-DD-<feature-name>/review-report.md`

The Reviewer Agent checks:

- Bugs
- Security issues
- Race conditions
- Distributed systems risks
- JVM/GC risks
- SQL optimization issues
- Bad abstractions
- Broken layering
- Missing tests
- Performance problems
- Redis misuse
- Kafka misuse
- Unauthorized architecture changes
- Production risks

The Reviewer Agent must review the implementation against the human-approved architecture.

---

## Validation Agent

The Validation Agent verifies that the implementation works correctly.

Main output:

- Spec validation: `specs/YYYY-MM-DD-<feature-name>/spec-validation-report.md`
- Implementation validation: `specs/YYYY-MM-DD-<feature-name>/validation-report.md`

The Validation Agent checks:

- Unit tests
- Integration tests
- API tests
- Edge cases
- Regression risks
- Docker Compose environment
- MySQL connectivity
- Redis connectivity
- Kafka connectivity if relevant
- Application startup
- Actuator health
- Roadmap acceptance criteria

The Validation Agent validates behavior. It does not approve architecture changes.

---

## Standard Workflow

1. Human Architect defines or approves a roadmap task.
2. Reviewer Agent performs feature-spec review and writes `spec-review-report.md`.
3. Validation Agent performs feature-spec validation and writes `spec-validation-report.md`.
4. Builder Agent implements one vertical slice.
5. Builder Agent updates `tasks.md`.
6. Reviewer Agent reviews the latest git diff and writes `review-report.md`.
7. Builder Agent fixes required review findings.
8. Validation Agent validates tests, edge cases, and acceptance criteria in `validation-report.md`.
9. Human Architect performs final review.
10. Human Architect commits, pushes, merges, or deploys.

---

## Git Rules

- Work should happen on a feature branch.
- Agents must not push code unless the human explicitly asks.
- Agents must not commit automatically unless the human explicitly asks.
- Agents must not merge code.
- Agents must not rewrite git history unless explicitly requested.
- Agents should keep changes small and reviewable.
- Agents should avoid unrelated file changes.

---

## Roadmap Rules

`specs/roadmap.md` tracks planned work and high-level status.

Use this marker style:

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS] Currently being worked on
- [ ] [BLOCKED] Blocked by dependency, missing decision, or environment issue
