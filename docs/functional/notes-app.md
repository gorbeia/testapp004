# Feature: Notes App

**Status:** In progress
**Last updated:** 2026-05-31

---

## Purpose

The Notes app is the initial application used to demonstrate the full architecture
in a working, end-to-end way. It is the baseline that all future features extend.

---

## Capability status

| Capability | Status | Notes |
|-----------|--------|-------|
| View list of notes | ✅ Done | Scrollable `LazyColumn` of cards |
| Empty state placeholder | ✅ Done | Shown when list is empty |
| Add note via dialog | ✅ Done | FAB → `AlertDialog` with title + content fields |
| Delete note | ✅ Done | Trash icon on each card; immediate, no confirmation |
| Persist notes across app restarts | ❌ Not started | Requires Room or DataStore (new ADR needed) |
| Edit existing note | ❌ Not started | — |
| Search / filter notes | ❌ Out of scope | See exclusions below |

---

## Data model

```kotlin
data class Note(
    val id: Long,          // System.currentTimeMillis() at creation time; unique enough for in-memory use
    val title: String,     // Required; must be non-blank after trimming
    val content: String,   // Optional; may be blank
    val createdAt: Long    // Timestamp (currently same value as id)
)
```

---

## Business rules

1. A note cannot be saved with a blank title. Whitespace-only input is treated as blank.
2. Title and content are trimmed of leading/trailing whitespace before saving.
3. Notes are displayed in insertion order (the order they were added).
4. Deleting a note is immediate and irreversible — no undo, no confirmation dialog.
5. Note content is optional — a note with a title and no content is valid.

---

## UX decisions

**Adding a note**
The FAB opens an `AlertDialog` modal on the same screen. Chosen over navigating to
a dedicated add-note screen because the input is simple (two fields) and the modal
keeps the user oriented in the list context.

**Deleting a note**
Single tap on the delete icon; no confirmation dialog. Optimises for speed. If undo
is needed in future it will be a separate decision (snackbar with undo action is the
likely approach — new functional doc when added).

**Empty state**
Displayed when the notes list is empty, with a text hint pointing to the FAB.
Avoids a blank screen with no affordance.

---

## Out of scope (explicitly excluded)

- Search, filter, or sort
- Note categories or tags
- Sharing notes externally
- Rich text / markdown formatting
- Reminders or notifications
- Multi-select or bulk actions

If any of these are added, update this document and create the relevant functional doc.
