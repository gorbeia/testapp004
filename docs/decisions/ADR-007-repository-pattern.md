# ADR-007: Repository Pattern for Data Access

**Date:** 2026-05-31
**Status:** Accepted

## Context

ViewModels previously owned their data directly (an in-memory list). This conflates UI state management with data access, making it impossible to swap storage backends (Room, DataStore, network) without rewriting the ViewModel. Tests also had no seam to replace data behavior.

## Decision

Introduce a `NotesRepository` interface in `data/` that all ViewModels depend on. Data access goes through this interface; ViewModels never touch storage directly.

```
ViewModel  →  NotesRepository (interface)
                  ↑
           InMemoryNotesRepository   (current, bound via Hilt)
           RoomNotesRepository       (future)
```

**Interface contract:**
- `notes: StateFlow<List<Note>>` — observable notes stream
- `suspend fun addNote(title, content)` — suspending writes
- `suspend fun deleteNote(noteId)` — suspending deletes

**Responsibilities:**
- Repository: stores and retrieves raw data, no validation
- ViewModel: validates input (blank title guard), trims whitespace, updates UI-only state (dialog open/close)

**Testing:**
- Unit tests use `FakeNotesRepository` (defined in `src/test/`) — no Hilt required
- Instrumented Compose UI tests use `InMemoryNotesRepository` directly

## Consequences

**Good:**
- Adding Room persistence later is a new class + binding change, not a ViewModel rewrite
- Clear boundary: data concerns stay in `data/`, UI concerns stay in `viewmodel/`

**Bad:**
- One more layer of indirection for a currently simple app
