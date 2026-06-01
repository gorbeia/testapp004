# testapp004 — Project Reference

Primary reference for Claude Code and human developers. Keep it current: whenever
an architectural or functional decision changes, update this file and the relevant
doc in `docs/`.

---

## Project Overview

Android Acquaintance Tracker app bootstrapped as a foundation for AI-assisted development.
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
| DI | Hilt + KSP | [ADR-006](docs/decisions/ADR-006-dependency-injection.md) |
| Data | Repository pattern + Room SQLite | [ADR-007](docs/decisions/ADR-007-repository-pattern.md), [ADR-013](docs/decisions/ADR-013-room-persistence.md) |
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
  ↑
ViewModel → Repository interface → Room*Repository (SQLite via Room)
```

Package layout:
```
com.example.testapp004/
├── MainActivity.kt
├── TestApp004Application.kt
├── data/           ← Repository interfaces + InMemory implementations
├── di/             ← Hilt modules
├── model/          ← plain Kotlin data classes
├── viewmodel/      ← ViewModels + UiState data classes
├── navigation/     ← NavHost + sealed Screen routes
└── ui/
    ├── screens/    ← one file per screen
    └── theme/      ← Color, Type, Theme
```

Package layout additions for Room:
```
data/
├── room/           ← @Entity classes, @Dao interfaces, AppDatabase, AcquaintanceWithCategories
├── Room*Repository ← Room-backed repository implementations
di/
├── ApplicationScope.kt  ← @Qualifier for the application-scoped CoroutineScope
└── DatabaseModule.kt    ← Provides AppDatabase, DAOs, ApplicationScope
```

---

## Conventions

- One ViewModel per screen; pass it as a parameter, never call `viewModel()` inside child composables
- Use `hiltViewModel()` (not `viewModel()`) in `AppNavigation.kt` to obtain ViewModels
- `UiState` is a `data class`; all screen state lives there — nothing ad-hoc
- `UiState` always includes `isLoading: Boolean = false` and `error: String? = null` for async operations
- No business logic inside Composables; they only observe state and call ViewModel methods
- Local ephemeral UI state (text field draft, scroll position) uses `remember { mutableStateOf() }`
- State mutations use `_uiState.update { }` for atomicity
- ViewModels use `viewModelScope.launch { }` to call `suspend` repository methods
- Navigation routes are a `sealed class Screen(val route: String)` in `navigation/AppNavigation.kt`
- Routes with arguments use `"{argName}"` in the route string; declare type via `navArgument`; extract via `backStackEntry.arguments`
- Repository interface lives in `data/`; Hilt bindings live in `di/AppModule.kt`
- Gradle dependencies go in `gradle/libs.versions.toml`; never hardcode versions in `build.gradle.kts`
- Unit tests use Fake*Repository + `MainDispatcherRule`; no Hilt setup needed in unit tests
- **Maximum line length is 120 characters** (enforced by ktlint). When any single line would exceed this — constructor calls, function arguments, lambda bodies, string concatenations — break it into multiple lines with each argument on its own line and a trailing comma. Check every new line before committing.

---

## Branch Workflow (for Claude)

**Never commit directly to `main`.** All work goes through a feature branch and a PR.

```
git checkout -b feature/short-description   # or fix/ or chore/
# ... make commits ...
git push -u origin feature/short-description
```

Then open a PR targeting `main`. CI must be green before the PR is merged.
The user reviews the diff and merges — do not merge your own PR.

Branch naming: `feature/` for new features, `fix/` for bug fixes, `chore/` for maintenance tasks.

---

## Before Every Push (for Claude)

**Always run this before pushing a branch:**

```
./gradlew test lint ktlintCheck assembleDebug --no-daemon --stacktrace
```

Fix every failure before the push. For ktlint violations, run `./gradlew ktlintFormat` first to auto-fix what it can, then re-run the check.
If Gradle is not available in the current environment, state that explicitly rather than skipping it.

---

## CI

GitHub Actions runs four parallel jobs on every push to `main` and on every pull request:

| Job | Gradle task | Fails when |
|-----|-------------|------------|
| `ktlint` | `ktlintCheck` | Code style violation |
| `unit-tests` | `test` | Logic regression |
| `lint` | `lint` | Android lint warning/error |
| `assemble` | `assembleDebug` | Compilation failure |

Jobs run in parallel. Each creates its own named check on the PR, so the failure type is visible without reading log output. A red CI build means something regressed — investigate before merging. Release workflow (`release.yml`) runs tests before building the signed APK.

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
| Acquaintance tracker — list people | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Acquaintance tracker — categories | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Acquaintance tracker — person detail & bio | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Acquaintance tracker — directed relations | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Acquaintance tracker — category trees | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Acquaintance tracker — multi-category membership | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Acquaintance tracker — predefined relation types | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Self-update (GitHub Releases) | ✅ Done | [self-update.md](docs/functional/self-update.md) |
| Android contact linking | ✅ Done | [android-contact-linking.md](docs/functional/android-contact-linking.md) |
| Category canvas view | ✅ Done | [ADR-017](docs/decisions/ADR-017-category-canvas-view.md) |
| Category tree browse | ✅ Done | [acquaintance-tracker.md](docs/functional/acquaintance-tracker.md) |
| Combined main screen (category browser + people search tabs) | ✅ Done | [ADR-018](docs/decisions/ADR-018-main-screen-tabs.md) |
| Add person from canvas (category pre-checked) | ✅ Done | [ADR-019](docs/decisions/ADR-019-add-person-from-canvas.md) |

---

## Decision Log

| ADR | Decision | Date |
|-----|----------|------|
| [ADR-001](docs/decisions/ADR-001-tech-stack.md) | Kotlin + Compose + MVVM as the tech stack | 2026-05-31 |
| [ADR-002](docs/decisions/ADR-002-architecture-pattern.md) | MVVM with StateFlow as the architecture pattern | 2026-05-31 |
| [ADR-003](docs/decisions/ADR-003-documentation-strategy.md) | Markdown ADRs + CLAUDE.md as the documentation strategy | 2026-05-31 |
| [ADR-004](docs/decisions/ADR-004-ci-strategy.md) | GitHub Actions CI running unit tests + lint on every push | 2026-05-31 |
| [ADR-005](docs/decisions/ADR-005-code-style.md) | ktlint for Kotlin code style enforcement | 2026-05-31 |
| [ADR-006](docs/decisions/ADR-006-dependency-injection.md) | Hilt + KSP for dependency injection | 2026-05-31 |
| [ADR-007](docs/decisions/ADR-007-repository-pattern.md) | Repository pattern separating ViewModels from data sources | 2026-05-31 |
| [ADR-008](docs/decisions/ADR-008-acquaintance-tracker.md) | Acquaintance tracker: people, categories, directed relations | 2026-05-31 |
| [ADR-009](docs/decisions/ADR-009-category-trees-multi-membership.md) | Category trees (parentId) and multi-category membership (categoryIds) | 2026-05-31 |
| [ADR-010](docs/decisions/ADR-010-self-update.md) | In-app self-update via GitHub Releases + OkHttp | 2026-05-31 |
| [ADR-011](docs/decisions/ADR-011-remove-notes-feature.md) | Remove Notes feature (bootstrap placeholder, superseded by Acquaintance Tracker) | 2026-05-31 |
| [ADR-012](docs/decisions/ADR-012-android-contact-linking.md) | Android contact linking via ContactsContract lookup key | 2026-05-31 |
| [ADR-013](docs/decisions/ADR-013-room-persistence.md) | Room SQLite persistence replacing in-memory storage | 2026-05-31 |
| [ADR-014](docs/decisions/ADR-014-debug-prerelease-update-channel.md) | Debug pre-release update channel via GitHub pre-releases + shared debug keystore | 2026-05-31 |
| [ADR-015](docs/decisions/ADR-015-room-gradle-plugin.md) | Room Gradle Plugin for variant-safe schema export (fixes KSP parallel-build race) | 2026-06-01 |
| [ADR-016](docs/decisions/ADR-016-predefined-relation-types.md) | Predefined relation types with perspective-aware labels (one row per relationship) | 2026-06-01 |
| [ADR-017](docs/decisions/ADR-017-category-canvas-view.md) | Category canvas view: pan/zoom graph of people and relations per category | 2026-06-01 |
| [ADR-018](docs/decisions/ADR-018-main-screen-tabs.md) | Combined main screen: category browser (default) + people search as two tabs | 2026-06-01 |
| [ADR-019](docs/decisions/ADR-019-add-person-from-canvas.md) | Add person from canvas FAB with canvas category pre-checked | 2026-06-01 |
