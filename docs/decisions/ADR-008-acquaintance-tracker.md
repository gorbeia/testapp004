# ADR-008 — Acquaintance Tracker feature

**Date:** 2026-05-31
**Status:** Accepted

---

## Context

The user wants to track acquaintances (people) with three capabilities:
1. Group them into user-defined categories
2. Keep a short bio/description on each person
3. Record directed, labeled relations between any two people (e.g. Alice →"mentors"→ Bob)

The app already has Notes. Acquaintances are a separate, parallel feature.

---

## Decision

### Data model

Three new plain Kotlin data classes in `model/`:

| Class | Fields |
|-------|--------|
| `Category` | `id: Long`, `name: String` |
| `Acquaintance` | `id: Long`, `name: String`, `bio: String`, `categoryId: Long?` |
| `Relation` | `id: Long`, `fromId: Long`, `toId: Long`, `label: String` |

Relations are directed (`fromId → label → toId`). The label is user-supplied free text.

### Storage

Three repository interfaces + in-memory implementations (same pattern as `NotesRepository`), bound via Hilt in `AppModule`. Persistent storage is deferred (see ADR-007).

### Navigation

A bottom navigation bar is added to `AppNavigation`, showing two tabs: **Notes** and **People**. The bottom bar is visible only on tab-root screens (`home`, `acquaintances`); it is hidden on all detail / form screens.

The outer `Scaffold` in `AppNavigation` uses `contentWindowInsets = WindowInsets(0.dp)` so that inner Scaffolds (one per screen) continue to manage their own status-bar insets via their `TopAppBar`. The outer `innerPadding` (bottom nav height only) is applied to the `NavHost` via `Modifier.padding(innerPadding)`.

### New screens

| Screen | ViewModel | Purpose |
|--------|-----------|---------|
| `AcquaintancesListScreen` | `AcquaintancesViewModel` | List people; filter chips per category |
| `AcquaintanceDetailScreen` | `AcquaintanceDetailViewModel` | View bio, category, relations; add/delete relations |
| `AddEditAcquaintanceScreen` | `AddEditAcquaintanceViewModel` | Create or edit a person (optional `acquaintanceId` nav arg; `-1` = new) |
| `CategoriesScreen` | `CategoriesViewModel` | Create / delete categories |

### ViewModels

`AcquaintancesViewModel` uses `kotlinx.coroutines.flow.combine` to merge the acquaintances flow, categories flow, and a `_selectedCategoryId` MutableStateFlow into a single `AcquaintancesUiState`. This keeps filtering reactive without ad-hoc state.

`AcquaintanceDetailViewModel` combines the three repository flows (acquaintances, categories, relations) and maps them to `RelationDisplay` items that carry `isOutgoing: Boolean` for direction-aware rendering. It reads `acquaintanceId` from `SavedStateHandle`.

`AddEditAcquaintanceViewModel` reads the optional `acquaintanceId` from `SavedStateHandle` (value `-1L` means create mode). On save it calls `addAcquaintance` or `updateAcquaintance` accordingly.

---

## Alternatives considered

* **Single notes-style bio list per person** (rejected) — the user asked for relations, which require a richer detail screen and a dedicated `Relation` model.
* **Undirected relations** — rejected in favour of directed + label per user preference.
* **Predefined categories** — rejected in favour of user-defined.
* **Tab-specific `NavHost`s** — single shared `NavHost` chosen for simplicity; state is in the repositories (singletons), so ViewModel recreation on tab switch is harmless.

---

## Consequences

* Three new Hilt singleton repositories; DI graph grows accordingly.
* The bottom navigation bar changes the app's visual structure; `HomeScreen` is unchanged but wrapped under a tab.
* `InMemoryXxxRepository` IDs use `System.currentTimeMillis()` (same as Notes) — collision-resistant for manual use, but fake repositories in tests use monotonic counters.
