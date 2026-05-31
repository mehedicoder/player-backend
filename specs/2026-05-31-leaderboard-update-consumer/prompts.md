# Prompt Log - 2026-05-31-leaderboard-update-consumer

## 2026-05-31 13:37 Europe/Berlin
use feature-spec skill.
create feature spec for the next feature on the roadmap.

## 2026-05-31 13:55 Europe/Berlin
1) Keep v1 small. It should consume only the event types needed to update leaderboard scores.

include:

- Kafka consumer for player activity events
- Consume reward/wallet/activity events relevant to leaderboard scoring
- Update leaderboard score table/read model
- Idempotent event processing using eventId
- Basic retry/error handling
- Tests for successful consumption
- Tests for duplicate event handling
- Tests for invalid/unsupported event handling


out of scope:

- Real-time WebSocket leaderboard updates
- Complex ranking algorithms
- Multiple leaderboard seasons
- Global/regional leaderboard separation
- DLQ replay tooling
- Backfill from historical events
- Separate leaderboard microservice
- Redis sorted-set leaderboard, unless already approved
- Admin leaderboard management UI


2)

A) MySQL leaderboard table + dedicated Kafka consumer group + idempotent processing


3)

A) Minimal shippable vertical slice

## 2026-05-31 14:13 Europe/Berlin
act as a reviewer agent. review the feature-spec

## 2026-05-31 14:16 Europe/Berlin
act as a builder agent .  use spec reviewer subagent. 
review the feature spec.

## 2026-05-31 14:23 Europe/Berlin
Act as a builder agent. use feature-spec skill to review spec reviewer subagent report and adapt the feature-spec.

## 2026-05-31 14:30 Europe/Berlin
review the spec again.

## 2026-05-31 14:32 Europe/Berlin
act as a validation agent. use spec validation subagent and validate the feature-spec.

## 2026-05-31 14:40 Europe/Berlin
finishing the remaining spec validation task.

## 2026-05-31 14:45 Europe/Berlin
act as a builder agent.

use subagents for best practices:

kafka-event-reviewer
security-reviewer
spring-boot-reviewer

implement the feature-spec

## 2026-05-31 14:55 Europe/Berlin
act as a builder agent.
implement the feature-spec

## 2026-05-31 17:31 Europe/Berlin
act as a reviewer agent.
use reviewer subagents to review the currently implemented feature

## 2026-05-31 17:36 Europe/Berlin
act as a builder agent and fix the issues report by reviewer agent.

## 2026-05-31 17:52 Europe/Berlin
act as a reviewer agent, use reviewer subagents and do a review again

## 2026-05-31 18:05 Europe/Berlin
now act as a builder agent and fix the issues reported on the reviewer's report

## 2026-05-31 18:42 Europe/Berlin
now review the implementation again as builder agent fixed the previously reported issues

## 2026-05-31 18:43 Europe/Berlin
act as a validation agent and validate the implementation

## 2026-05-31 19:00 Europe/Berlin
act as a builder agent, read the validation-report.md , find the blocker and other issues and fix them. After that perform a validation again

## 2026-05-31 19:13 Europe/Berlin
act as a builder agent and take care of the issues reported by the reviewer agent

## 2026-05-31 19:15 Europe/Berlin
now take care of the residual gap reported by validation agent

## 2026-05-31 19:19 Europe/Berlin
use git push skill . create PR
