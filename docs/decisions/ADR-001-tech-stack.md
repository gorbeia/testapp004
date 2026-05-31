# ADR-001: Kotlin + Jetpack Compose + MVVM as the primary tech stack

**Date:** 2026-05-31
**Status:** Accepted

## Context

A new Android repository needed to be bootstrapped with a tech stack suitable for
ongoing AI-assisted development with Claude Code. Requirements:

- Claude Code can generate, read, and reason about the code with high reliability
- Represents current Android best practices so the codebase stays relevant
- Productive for human developers reviewing and extending Claude's output
- Minimal ceremony: the stack should not require large amounts of boilerplate

## Decision

Use the following stack:

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **State**: ViewModel + StateFlow (MVVM pattern)
- **Navigation**: Compose Navigation with sealed class routes
- **Build**: Gradle Kotlin DSL (`.kts`) with a version catalog (`gradle/libs.versions.toml`)
- **Min SDK**: 24 (Android 7.0, ~95%+ active device coverage)
- **Target/Compile SDK**: 34 (Android 14)

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| XML layouts (View system) | Imperative and verbose; AI-generated XML is error-prone and hard to validate; Compose is the current Android standard |
| Java | Less expressive than Kotlin; no coroutines; not the modern Android standard |
| React Native or Flutter | Cross-platform adds unnecessary complexity; the goal is a native Android app |
| Hilt (dependency injection) | Adds build complexity and annotation processing overhead; deferred until the app grows enough to justify it |
| Room (local database) | Not needed for the initial bootstrap; in-memory state is sufficient for a dummy app |
| Groovy Gradle DSL | No type safety; Kotlin DSL is the current Android Studio default |

## Consequences

- All UI is written in Kotlin (Compose DSL), no XML layouts
- No XML resource drawables for layouts — only for icons and adaptive launcher icons
- Business state is always in `ViewModel` + `StateFlow`; never in `mutableStateOf` at the screen level
- All Gradle files are `.kts`; all dependency versions live in `gradle/libs.versions.toml`
- DI and persistence are out of scope until explicitly decided (will require new ADRs)
