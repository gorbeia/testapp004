# Backup & restore (Google Drive)

## Behaviour
- Menu (⋮ on the main screen) → **Backup & restore**.
- **Back up now** serializes all people, categories, relations and category memberships to one JSON file and
  uploads it to the hidden, app-private Google Drive `appDataFolder`. The file is overwritten on each backup
  (one backup, no history). Users never see it in their Drive.
- **Restore from backup** downloads that file and **replaces** all local data in a single transaction, after an
  explicit confirmation dialog. A missing, corrupt or newer-schema backup is rejected and local data is untouched.
- The first use shows Google's consent screen for the `drive.appdata` scope; the action then continues automatically.

## Format
`{"format":1,"schemaVersion":<Room version>,"createdAt":<ms>,"acquaintances":[…],"categories":[…],
"relations":[…],"crossRefs":[…]}`. Optional fields are defaulted when absent so backups from older schema versions
stay restorable. Row ids are preserved so relations and memberships stay consistent.

## Out of scope (see ADR-050)
Automatic/scheduled backup, multiple backup versions, and cross-device merge sync.

## One-time setup (outside the code)
1. Google Cloud project with the **Google Drive API** enabled and an OAuth consent screen.
2. An **Android OAuth client ID** for package `com.example.testapp004` for each signing key's SHA-1
   (shared debug keystore and the release keystore). No client secret or ID is put in the app.
