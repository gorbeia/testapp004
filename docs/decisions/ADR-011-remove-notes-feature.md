# ADR-011: Remove Notes Feature

**Date:** 2026-05-31  
**Status:** Accepted

## Context

The app was bootstrapped with a simple Notes feature (list, add, delete, edit) as a scaffolding baseline to establish the architecture. As the Acquaintance Tracker became the primary feature, the Notes feature had no remaining purpose — it was dummy/placeholder content that added dead code, unnecessary navigation complexity, and misleading bottom-nav tabs.

## Decision

Remove the entire Notes feature:

- Deleted: `model/Note.kt`, `data/NotesRepository.kt`, `data/InMemoryNotesRepository.kt`, `viewmodel/NotesViewModel.kt`, `ui/screens/HomeScreen.kt`, `ui/screens/EditNoteScreen.kt`
- Deleted: corresponding unit tests (`NotesViewModelTest.kt`, `FakeNotesRepository.kt`) and UI test (`HomeScreenTest.kt`)
- Removed: bottom navigation bar (only one top-level screen remains)
- Updated: `AppNavigation.kt` — `AcquaintancesList` is now the start destination; `Screen.Home` and `Screen.EditNote` routes removed
- Updated: `di/AppModule.kt` — `bindNotesRepository` binding removed

## Consequences

- The app now launches directly into the Acquaintance Tracker list.
- No bottom navigation bar is shown (single top-level destination).
- Codebase is smaller and focused solely on the Acquaintance Tracker.
- If a Notes feature is reintroduced in future, it should be built fresh with persistent storage rather than resurrecting in-memory scaffolding.
