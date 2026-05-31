# ADR-002: MVVM with unidirectional data flow

**Date:** 2026-05-31
**Status:** Accepted

## Context

The app needs a clear, consistent pattern for separating UI rendering from
business logic and state management. The chosen pattern must be:

- Testable: ViewModel logic must be unit-testable without an Android runtime
- Predictable: given a state, the UI renders deterministically
- Easy for Claude to follow: a consistent pattern means Claude can extend any
  screen without reading the others first

## Decision

MVVM with strict unidirectional data flow:

1. Each screen has one `ViewModel` that owns all non-ephemeral state
2. State is a single `data class` named `<Screen>UiState`, exposed as `StateFlow<UiState>`
3. The Composable observes state via `collectAsState()` and renders it
4. User interactions call ViewModel methods — the Composable never mutates state directly
5. Only local, transient UI state (e.g., a text field draft before submission) lives in
   `remember { mutableStateOf() }` inside the Composable

```
User interaction
  → Composable calls viewModel.someMethod()
    → ViewModel mutates _uiState via update { }
      → StateFlow emits new UiState
        → Composable recomposes
```

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| MVI (Model-View-Intent) with sealed Intent classes | More boilerplate for no measurable benefit at this scale; can migrate if the app grows large enough |
| MVP | Not idiomatic with Compose's reactive model |
| State directly in Composables | Untestable; breaks separation of concerns; makes screens hard to refactor |
| Redux-style global store | Overkill; imposes a framework on top of what Jetpack provides natively |
| Multiple StateFlows per ViewModel | Scattered state is harder to reason about; a single UiState snapshot is easier to test and debug |

## Consequences

- Every screen maps to exactly one ViewModel and one `UiState` data class
- ViewModels are plain Kotlin classes (extend `androidx.lifecycle.ViewModel`); they can be
  instantiated directly in unit tests without Robolectric or Hilt
- Adding a new UI feature always follows the same sequence:
  1. Add a field to `UiState`
  2. Add a method to the ViewModel
  3. Update the Composable to reflect the new state / call the new method
- Composables that need shared state receive the ViewModel as a parameter (created once
  at the NavHost level), not via a local `viewModel()` call
