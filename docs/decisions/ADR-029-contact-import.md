# ADR-029: Contact Import Feature

**Date**: 2026-06-03  
**Status**: Accepted

## Context

The app already supports linking an existing person to an Android contact (ADR-012). The inverse — creating a new person from an existing Android contact — was missing. Users managing a large contacts list wanted a way to bootstrap their acquaintance tracker from existing data.

## Decision

Add a **contact import** flow that:

1. Reads all contacts from `ContactsContract` (requires `READ_CONTACTS` permission, already in the manifest).
2. Shows a multi-select list with deduplication: contacts already linked to a person are greyed out and non-selectable.
3. Lets the user optionally assign imported people to one or more categories before confirming.
4. Creates one `Acquaintance` per selected contact, auto-linking it via `androidContactLookupKey`.
5. Populates the following fields from contact data:
   - `name` ← `DISPLAY_NAME_PRIMARY`
   - `birthday` ← `CommonDataKinds.Event` (type = `TYPE_BIRTHDAY`), stored as ISO-8601 string
   - `bio` ← `CommonDataKinds.Organization` (title + company) and `CommonDataKinds.Note`, joined with a newline

### New `birthday` field

`Acquaintance` gains a nullable `birthday: String?` field storing an ISO-8601 date ("YYYY-MM-DD") or a no-year variant ("--MM-DD") as returned by `ContactsContract`. This is a Room migration (v3 → v4). Birthday is displayed in `AcquaintanceDetailScreen` with a cake icon; editing is not exposed in the add/edit form (it is preserved on save to avoid data loss).

### Data fetching strategy

Four batched queries replace per-contact N+1 calls: one for contacts, one for birthdays, one for organisations, one for notes. Results are joined in memory.

## Alternatives Considered

- **`java.time.LocalDate` for birthday**: Requires API 26 or core library desugaring. Storing as a plain `String` avoids the dependency while preserving the value faithfully.
- **Import phone/email**: No structured field exists in the model; dumping into bio would be lossy. Deferred.
- **Ongoing sync**: Out of scope. Import is a one-shot operation.
- **`CommonDataKinds.Relation` auto-linking**: Interesting but complex; deferred to a separate feature.

## Consequences

- Room schema bumped to version 4; `MIGRATION_3_4` adds a nullable `birthday TEXT` column.
- `AcquaintanceRepository.addAcquaintance` gains two optional parameters (`birthday`, `androidContactLookupKey`) with `null` defaults; all existing callers are unaffected.
- `ContactRepository` gains `fetchImportableContacts()`, backed by a new `ImportableContact` model class.
- Entry point: "Import from contacts" in the overflow menu of the main screen.
