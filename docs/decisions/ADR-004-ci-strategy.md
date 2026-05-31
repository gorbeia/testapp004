# ADR-004: GitHub Actions CI running unit tests and lint on every push

**Date:** 2026-05-31
**Status:** Accepted

## Context

The project is developed through AI-assisted vibe coding. Claude generates changes
quickly but has no persistent memory between sessions and no way to verify that a
new change hasn't broken existing behaviour unless it explicitly runs the tests.
Without automated CI, regressions can reach `main` silently.

## Decision

Add a GitHub Actions workflow (`.github/workflows/ci.yml`) that runs on every push
to `main` and every pull request targeting `main`. The workflow:

1. Runs `./gradlew test` — all JVM unit tests must pass
2. Runs `./gradlew lint` — lint report is uploaded as an artifact on every run

Unit tests run on the JVM (no emulator needed), making them fast and cheap.
Instrumented UI tests are excluded for now because they require an Android emulator,
which adds significant CI time and cost.

`CLAUDE.md` is updated to instruct Claude to run `./gradlew test` locally before
every push, making CI the safety net for cases where that step is skipped or
the environment doesn't support it.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| No CI | Regressions reach `main` silently; defeats the purpose of having tests |
| Instrumented tests in CI | Require an emulator; significantly slower and more expensive; deferred until there is UI test coverage worth running |
| Firebase Test Lab / Browserstack | External service cost and complexity; not justified at this stage |
| Running lint as a required check only | Tests are the primary regression guard; lint catches style drift that tests won't |

## Consequences

- Every push to `main` is verified by unit tests and lint automatically
- A failing CI build is a signal to investigate before merging
- Claude is instructed in `CLAUDE.md` to run `./gradlew test` before pushing
- When instrumented/UI tests are added, the workflow can be extended with an emulator step (new ADR not required — it's an extension of this decision)
