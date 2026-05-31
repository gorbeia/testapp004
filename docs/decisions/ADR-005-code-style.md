# ADR-005: ktlint for Kotlin code style enforcement

**Date:** 2026-05-31
**Status:** Accepted

## Context

In AI-assisted vibe coding, code style drifts between sessions because Claude has
no persistent memory of stylistic choices made previously. Over time this produces
inconsistent indentation, import ordering, and formatting that accumulates into
noise that makes diffs harder to read and review.

A linter enforcing a fixed style automatically removes this drift — Claude is forced
to conform on every push regardless of what it "remembered" from earlier sessions.

## Decision

Use [ktlint](https://pinterest.github.io/ktlint/) via the
`jlleitschuh/gradle-ktlint` Gradle plugin (`12.1.1`).

Configuration lives in `.editorconfig` at the repository root. Deviations from
the default ruleset:

- `ktlint_standard_no-wildcard-imports = disabled` — Jetpack Compose uses wildcard
  imports extensively (e.g. `import androidx.compose.material3.*`); enforcing explicit
  imports would produce excessive noise without meaningful benefit at this stage.

CI runs `./gradlew ktlintCheck` as part of the combined build step. The build fails
if any Kotlin file violates the style rules.

For local use, `./gradlew ktlintFormat` auto-fixes the majority of violations before
`ktlintCheck` is run.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Detekt | Static analysis tool, not a formatter; complementary to ktlint but heavier to configure; deferred |
| Android Lint only | Catches Android-specific issues, not general Kotlin style; already included separately |
| No style enforcement | Style drift accumulates across sessions; diffs become noisy; rejected |
| Spotless | More general formatting tool; ktlint is the Kotlin-specific standard |

## Consequences

- Every Kotlin file in `:app` is checked on every CI run
- Style violations block merging (CI fails)
- Claude must run `./gradlew ktlintFormat` before pushing when violations exist
- The `.editorconfig` is the single source of truth for style rules — any rule change goes there
- If wildcard imports become a problem (ambiguity, unused symbols), re-enable
  `ktlint_standard_no-wildcard-imports` and fix the imports project-wide
