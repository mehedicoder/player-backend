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

## Prompt Logging

Prompts must be stored by scope:

- General/non-feature prompts: `prompts/YEAR-MONTH-DATE.md`
- Feature-specific prompts: `specs/YYYY-MM-DD-<feature-name>/prompts.md`

### Classification Rule

- Feature-specific: prompts tied to a specific feature folder or phase implementation/review/validation.
- General/non-feature: prompts about global workflow, role prompts, repo conventions, or governance not tied to one feature folder.

### General Prompt Log Rules

- Log the prompt immediately when a new user instruction is received, before any implementation steps.
- Use existing `prompts/YEAR-MONTH-DATE.md` if present.
- If missing, create it using `prompts/2026-05-22.md` style.
- Header:
  - `# Prompt Log - YEAR-MONTH-DATE`
- Entries:
  - `## YEAR-MONTH-DATE HH:MM Europe/Berlin`
  - prompt text on next line(s)
- Keep entries concise and chronological (oldest to newest).

### Feature Prompt Log Rules

- Log the prompt immediately when a new feature-specific instruction is received, before implementation/review/validation work starts.
- Store in active feature folder as `specs/YYYY-MM-DD-<feature-name>/prompts.md`.
- Create file if missing.
- Header:
  - `# Prompt Log - YYYY-MM-DD-<feature-name>`
- Entries:
  - `## YEAR-MONTH-DATE HH:MM Europe/Berlin`
  - prompt text on next line(s)
- Keep entries concise and chronological (oldest to newest).
- Do not duplicate feature-specific entries in `prompts/YEAR-MONTH-DATE.md`.

### Prompt Logging Checklist (Mandatory)

Before closing any task, verify:
- Today's general file exists: `prompts/YEAR-MONTH-DATE.md` (if any general prompts occurred).
- Active feature `prompts.md` exists (if feature-specific prompts occurred).
- New prompts were appended with timestamp format:
  - `## YEAR-MONTH-DATE HH:MM Europe/Berlin`

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
- Writing `specs/YYYY-MM-DD-<feature-name>/spec-review-report.md` for feature-spec reviews
- Writing `specs/YYYY-MM-DD-<feature-name>/review-report.md` for implementation reviews

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
- Writing `specs/YYYY-MM-DD-<feature-name>/spec-validation-report.md` for feature-spec validation
- Writing `specs/YYYY-MM-DD-<feature-name>/validation-report.md` for implementation validation

The Validation Agent should not implement features unless explicitly requested.

### Workflow Guide

Use this file for the overall workflow rules:

- `prompts/agent-workflow.md`

---

## Default Workflow

Use this workflow unless the human asks otherwise:

1. Human selects or approves a roadmap task.
2. Builder Agent create feature specification using the skill feature-spec from /skills/feature-spec folder
3. Reviewer Agent review the feature-spec and generate spec-review-report on the feature folder.
4. Builder Agent check the spec-review-report.md and fix any issue reported.
5. Reviewer Agent review the feature spec again and update spec-review-report on the feature folder.
6. Validator Agent validate the feature spec and generate spec-validation-report.md.
7. Builder Agent review the spec-validation-report.md and address any issue reported.
6. Builder Agent implements the task.
7. Builder Agent updates `tasks.md`.
8. Reviewer Agent reviews the latest git diff.
9. Builder Agent fixes required review findings.
10. Validation Agent validates tests and acceptance criteria.
11. Human performs final review.
12. Human decides whether to commit, push, merge, or deploy.

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

## Report Naming Transition

Current report naming standard (per feature folder):
- `spec-review-report.md`
- `spec-validation-report.md`
- `review-report.md`
- `validation-report.md`

If older feature folders only contain `review-report.md` and/or `validation-report.md`,
do not rename historical files unless explicitly requested.

Use this roadmap marker style:

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS] Currently being worked on
- [ ] [BLOCKED] Blocked by dependency, missing decision, or environment issue
