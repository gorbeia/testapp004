# ADR-003: Markdown ADRs + CLAUDE.md as the documentation strategy

**Date:** 2026-05-31
**Status:** Accepted

## Context

The project is developed collaboratively between humans and Claude Code across multiple
sessions. Each session starts with Claude having no memory of prior conversations.
Without a systematic approach:

- Decisions made in one session are invisible in the next
- Claude repeats already-rejected alternatives
- Humans lose track of what was decided and why
- The codebase drifts from its intended architecture without anyone noticing

## Decision

Maintain two documentation layers, both version-controlled in the repository:

**Layer 1 — `CLAUDE.md` (root)**
Always-in-context summary. Claude Code reads this file automatically at the start
of every session. Contains: project overview, tech stack table, architecture summary,
coding conventions, feature status table, and a decision log index.
This file is the entry point — it tells Claude what to look up, not everything itself.

**Layer 2 — `docs/`**
Detailed documentation, linked from CLAUDE.md:
- `docs/decisions/ADR-NNN-title.md` — one file per significant technical decision
- `docs/functional/<feature>.md` — one file per feature area, describing scope and rules
- `docs/architecture.md` — full architecture reference

**Update protocol for Claude:**
Documentation is written as part of the same commit as the implementation — not as a
follow-up. The rule is:

| Type of decision | What to write |
|-----------------|---------------|
| Technical (library, pattern, tooling, architecture) | New ADR in `docs/decisions/` + row in CLAUDE.md decision log |
| Functional (feature scope, UX behaviour, business rule) | New or updated doc in `docs/functional/` + updated feature table in CLAUDE.md |
| Both | Both |

ADRs use sequential numbering (ADR-001, ADR-002, …) and are never edited after
acceptance. If a decision changes, the old ADR is marked "Superseded by ADR-NNN"
and a new ADR is created.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| GitHub Wiki | Not in the repository; Claude cannot read or write it |
| Inline code comments | Too granular; not visible across the codebase; lost when code is deleted |
| Single large README | Becomes unwieldy quickly; hard to update incrementally; mixes concerns |
| Decision log only in chat history | Lost between sessions; inaccessible to Claude in new sessions |
| External tool (Notion, Confluence) | Outside the repo; Claude cannot access it; adds tooling dependency |

## Consequences

- Every PR that involves a decision must include doc updates — reviewers should check this
- `CLAUDE.md` is the first file Claude reads; it must be kept accurate or Claude will act on stale information
- ADRs accumulate over time; the decision log table in CLAUDE.md keeps them navigable
- Functional docs capture scope explicitly, including what is out of scope, to prevent feature creep
