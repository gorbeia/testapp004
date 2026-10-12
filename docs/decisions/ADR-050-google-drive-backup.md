# ADR-050: Google Drive backup & restore

**Date:** 2026-10-12
**Status:** Accepted

## Context
Users want to back up their data and later move it between devices. Data lives in Room (SQLite).

## Decision
- Store one JSON snapshot in Drive's hidden `appDataFolder` (`drive.appdata` scope — app-private, non-sensitive).
- Authorize with `play-services-auth` `AuthorizationClient`; call the Drive v3 REST API with the existing OkHttp
  client rather than adding the heavy Google API client library.
- JSON (via `org.json`) instead of a raw DB copy so backups survive schema migrations; the snapshot records the
  Room version and a newer-schema backup is refused.
- Layering: `BackupRepository` → `DriveAuthorizer`, `BackupRemoteStore`, `BackupLocalStore` (all faked in tests).
  Restore runs in one Room transaction with `defer_foreign_keys` so self-referencing categories insert cleanly.
- `AppDatabase.VERSION` constant is the single source for the Room version used by backups.

## Consequences
- Manual backup/restore only; restore is destructive (confirmed in UI). Last backup wins (single file).
- Cross-device sync with merge (per-record UUIDs, `updatedAt`, tombstones) is a possible follow-up.
- Requires one-time Google Cloud OAuth client setup per signing key (see functional doc).
