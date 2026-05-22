# AGENTS.md

This repository uses a human-led, role-based agentic workflow.

There is no Architect Agent. The human engineer is the architect.

## Human-Owned Decisions

The human engineer owns:

- Architecture decisions
- Service boundaries
- Database design
- Redis usage
- Kafka usage
- Consistency model
- Security boundaries
- Production trade-offs
- Final review
- Commit, push, merge, and deployment decisions

Agents may suggest improvements, risks, or alternatives, but they must not make major architecture decisions without explicit human approval.

---

## Project Context

Before doing any work, read the relevant project documents:

- `specs/mission.md`
- `specs/tech-stack.md`
- `specs/roadmap.md`
- `specs/architecture.md` if available
- `specs/api-contract.md` if available
- `specs/database-design.md` if available
- `specs/local-development.md` if available
- `tasks.md` if available

---

## Available Agent Roles

This project has role-specific prompt files under `prompts/`.

Use the correct role file based on the task.

### Builder Agent

Use this file when implementing code:

- `prompts/builder-agent.md`

Use the Builder Agent for:

- Feature implementation
- Bug fixes
- Tests
- Configuration changes
- Database migrations
- Docker Compose changes
- Documentation updates related to implementation

The Builder Agent must implement only one approved roadmap task or one vertical slice at a time.

### Reviewer Agent

Use this file when reviewing implementation:

- `prompts/reviewer-agent.md`

Use the Reviewer Agent for:

- Reviewing latest git diff
- Finding bugs
- Finding security issues
- Finding concurrency risks
- Finding distributed systems risks
- Finding JVM/GC risks
- Finding SQL optimization issues
- Finding Redis/Kafka misuse
- Checking architecture compliance
- Checking missing tests
- Writing `reports/review-report.md`

The Reviewer Agent should not implement code unless explicitly requested.

### Validation Agent

Use this file when validating implementation:

- `prompts/validation-agent.md`

Use the Validation Agent for:

- Running or checking tests
- Validating acceptance criteria
- Checking edge cases
- Checking regression risks
- Checking Docker Compose environment
- Checking MySQL/Redis/Kafka connectivity where relevant
- Checking application startup
- Writing `reports/validation-report.md`

The Validation Agent should not implement features unless explicitly requested.

### Workflow Guide

Use this file for the overall workflow rules:

- `prompts/agent-workflow.md`

---

## Default Workflow

Use this workflow unless the human asks otherwise:

1. Human selects or approves a roadmap task.
2. Builder Agent implements the task.
3. Builder Agent updates `docs/tasks.md`.
4. Reviewer Agent reviews the latest git diff.
5. Builder Agent fixes required review findings.
6. Validation Agent validates tests and acceptance criteria.
7. Human performs final review.
8. Human decides whether to commit, push, merge, or deploy.

---

## How to Activate a Role

When the human says:

- "Act as Builder Agent"
- "Use Builder Agent"
- "Run Builder Agent"

Then read and follow:

- `prompts/builder-agent.md`

When the human says:

- "Act as Reviewer Agent"
- "Use Reviewer Agent"
- "Run Reviewer Agent"
- "Review this diff"

Then read and follow:

- `prompts/reviewer-agent.md`

When the human says:

- "Act as Validation Agent"
- "Use Validation Agent"
- "Run Validation Agent"
- "Validate this implementation"

Then read and follow:

- `prompts/validation-agent.md`

If the requested role is unclear, ask which role to use before changing files.

---

## Safety Rules

Agents must not:

- Push code unless explicitly asked
- Commit automatically unless explicitly asked
- Merge branches
- Deploy
- Rewrite git history unless explicitly asked
- Store secrets in the repository
- Remove tests to make a build pass
- Hide failures
- Mark roadmap tasks complete without validation evidence
- Introduce new services, databases, queues, frameworks, or infrastructure components without explicit human approval

---

## Roadmap and Task Tracking

`specs/roadmap.md` tracks planned work and high-level status.

`tasks.md` records completed work and implementation history.

Use this roadmap marker style:

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS] Currently being worked on
- [ ] [BLOCKED] Blocked by dependency, missing decision, or environment issue