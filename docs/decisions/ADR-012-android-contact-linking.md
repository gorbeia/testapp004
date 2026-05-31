# ADR-012: Android Contact Linking

**Date:** 2026-05-31
**Status:** Accepted

## Context

Each acquaintance in the tracker can optionally correspond to a real person in
the device's Android Contacts. Surfacing the contact's phone number directly in
the detail screen removes the need to switch apps and keeps the tracker useful
as a daily-driver tool.

## Decision

- Add an optional `androidContactLookupKey: String?` field to `Acquaintance`.
  The lookup key (not the raw contact ID) is used because it survives contact
  merges and syncs.
- Introduce a `ContactRepository` interface with `lookupContactByUri` and
  `lookupContactByKey`, backed by `AndroidContactRepository` which queries
  `ContactsContract` via `ContentResolver` on the IO dispatcher.
- The system contact picker (`ActivityResultContracts.PickContact`) is launched
  from the detail screen. `READ_CONTACTS` is requested at runtime before the
  first pick (min SDK 24 ≥ API 23 where runtime permissions are required).
- Link/unlink actions live on `AcquaintanceDetailViewModel`; the composable
  holds only the launcher references and calls ViewModel methods.
- `AcquaintanceDetailUiState` gains a `linkedContactInfo: ContactInfo?` field.
  The ViewModel watches for lookup-key changes in the combine collector and
  re-fetches contact info only when the key actually changes.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Store raw contact ID (Long) | Contact IDs can change after merges or re-syncs; lookup key is the stable identifier recommended by the Android docs |
| Store full contact URI as String | Encodes both ID and lookup key — redundant; lookup key alone is sufficient and cleaner to persist |
| Fetch contact info inside the Composable | Violates MVVM — no business/data logic in composables |
| Accompanist Permissions library | Extra dependency not justified; a two-line `checkSelfPermission` + launcher achieves the same result |

## Consequences

- `READ_CONTACTS` runtime permission is required; users who deny it cannot link
  contacts but the app continues to function normally.
- `AndroidContactRepository` is not unit-testable against the real
  `ContentResolver`; tests should use a fake/stub `ContactRepository`.
- If a linked contact is later deleted from the device, `lookupContactByKey`
  returns `null`; the UI shows "Contact unavailable" and offers an Unlink button.
- A future Room migration will need to persist `androidContactLookupKey`.
