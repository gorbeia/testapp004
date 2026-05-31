# testapp004 — Project Reference

Primary reference for Claude Code and human developers. Keep it current: whenever
an architectural or functional decision changes, update this file and the relevant
doc in `docs/`.

---

## Project Overview

Android Notes app bootstrapped as a foundation for AI-assisted development.
The app is intentionally simple — it demonstrates the architecture and provides
a working baseline that is extended feature by feature in collaboration with Claude.

---

## Tech Stack

| Layer | Technology | ADR |
|-------|-----------|-----|
| Language | Kotlin | [ADR-001](docs/decisions/ADR-001-tech-stack.md) |
| UI | Jetpack Compose + Material3 | [ADR-001](docs/decisions/ADR-001-tech-stack.md) |
| State | ViewModel + StateFlow | [ADR-002](docs/decisions/ADR-002-architecture-pattern.md) |
| Navigation | Compose Navigation (sealed routes) | [ADR-002](docs/decisions/ADR-002-architecture-pattern.md) |
| Build | Gradle Kotlin DSL + Version Catalog | [ADR-001](docs/decisions/ADR-001-tech-stack.md) |
| Min SDK | 24 (Android 7.0) | [ADR-001](docs/decisions/ADR-001-tech-stack.md) |
| Target/Compile SDK | 34 (Android 14) | [ADR-001](docs/decisions/ADR-001-tech-stack.md) |

---

## Architecture

MVVM with strict unidirectional data flow. See [docs/architecture.md](docs/architecture.md).

```
UI (Composable screens)
  ↓ user events → ViewModel methods
ViewModel
  ↓ StateFlow<UiState>
UI (re-renders on state change)
```

Package layout:
```
com.example.testapp004/
├── MainActivity.kt
├── model/          ← plain Kotlin data classes
├── viewmodel/      ← ViewModels + UiState data classes
├── navigation/     ← NavHost + sealed Screen routes
└── ui/
    ├── screens/    ← one file per screen
    └── theme/      ← Color, Type, Theme
```

---

## Conventions

- One ViewModel per screen; pass it as a parameter, never call `viewModel()` inside child composables
- `UiState` is a `data class`; all screen state lives there — nothing ad-hoc
- No business logic inside Composables; they only observe state and call ViewModel methods
- Local ephemeral UI state (text field draft, scroll position) uses `remember { mutableStateOf() }`
- State mutations use `_uiState.update { }` for atomicity
- Navigation routes are a `sealed class Screen(val route: String)` in `navigation/AppNavigation.kt`
- Gradle dependencies go in `gradle/libs.versions.toml`; never hardcode versions in `build.gradle.kts`

---

## Before Every Push (for Claude)

**Always run this before committing and pushing:**

```
./gradlew test
```

Fix every failure before the commit goes out. If `./gradlew test` is not available in the current environment, state that explicitly rather than skipping it.

---

## CI

GitHub Actions runs `./gradlew test` and `./gradlew lint` on every push to `main` and on every pull request. A red CI build means something regressed — investigate before merging.

Workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

---

## Documentation Rules (for Claude)

**Every decision made in a conversation must be recorded before the commit is pushed.**

| What happened | What to update |
|--------------|----------------|
| Chose a library, pattern, or tool | Create `docs/decisions/ADR-NNN-title.md`; add a row to the Decision Log below |
| Added or changed a feature's scope or behaviour | Create or update `docs/functional/<feature>.md` |
| Changed a top-level convention or the architecture | Update this file + the relevant ADR |

Use the template at [docs/decisions/TEMPLATE.md](docs/decisions/TEMPLATE.md).
ADRs are append-only: never edit a settled ADR; mark it "Superseded by ADR-NNN" and write a new one.

---

## Current Features

| Feature | Status | Functional spec |
|---------|--------|----------------|
| View notes list | ✅ Done | [notes-app.md](docs/functional/notes-app.md) |
| Empty state placeholder | ✅ Done | [notes-app.md](docs/functional/notes-app.md) |
| Add note via dialog | ✅ Done | [notes-app.md](docs/functional/notes-app.md) |
| Delete note | ✅ Done | [notes-app.md](docs/functional/notes-app.md) |
| Persistent storage | ❌ Not started | — |
| Edit existing note | ❌ Not started | — |

---

## Decision Log

| ADR | Decision | Date |
|-----|----------|------|
| [ADR-001](docs/decisions/ADR-001-tech-stack.md) | Kotlin + Compose + MVVM as the tech stack | 2026-05-31 |
| [ADR-002](docs/decisions/ADR-002-architecture-pattern.md) | MVVM with StateFlow as the architecture pattern | 2026-05-31 |
| [ADR-003](docs/decisions/ADR-003-documentation-strategy.md) | Markdown ADRs + CLAUDE.md as the documentation strategy | 2026-05-31 |
| [ADR-004](docs/decisions/ADR-004-ci-strategy.md) | GitHub Actions CI running unit tests + lint on every push | 2026-05-31 |
