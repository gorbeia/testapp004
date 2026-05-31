# ADR-014 — Debug pre-release update channel via GitHub pre-releases

**Date:** 2026-05-31
**Status:** Accepted

## Context

ADR-010 introduced self-update via `/releases/latest`, which only returns
non-pre-release GitHub Releases. In practice this means the banner never
appears on debug builds because:

1. No production release has been tagged yet.
2. Even when one is tagged, a production-signed APK cannot install over a
   debug-signed APK (Android enforces signing-certificate consistency).

Developers running debug builds need a parallel update channel so the full
download → install flow can be exercised without going through production
tagging.

## Decision

Add a dedicated `debug-latest` GitHub pre-release that is updated
automatically on every push to `main`:

- A new CI workflow (`debug-prerelease.yml`) builds the debug APK with a
  version stamped `1.0.<run_number>` (always newer than the installed `1.0`),
  using a shared debug keystore committed to the repository at
  `keystores/debug.keystore`, then deletes and recreates the `debug-latest`
  pre-release with the new APK.
- `GitHubUpdateRepository` checks
  `/releases/tags/debug-latest` when `BuildConfig.DEBUG` is true, and
  `/releases/latest` otherwise.

The debug keystore must be the same one used to sideload the installed APK
(typically `~/.android/debug.keystore`). Its default credentials — store
password `android`, alias `androiddebugkey`, key password `android` — are
well-known and acceptable for a non-production signing artifact.

## Alternatives Considered

| Option | Why rejected |
|--------|-------------|
| Keep fake `AppRelease` in `UpdateViewModel` | Only tests the banner UI, not the download or install path |
| Upload debug APK to the same production release | Mixes signing keys; production users would download an uninstallable APK |
| Firebase App Distribution | External SDK dependency; overkill for an internal debug flow |

## Consequences

- The repository includes `keystores/debug.keystore` (the developer's local
  debug keystore). Each developer must replace this file with their own
  `~/.android/debug.keystore` so that locally sideloaded APKs and CI-built
  APKs share the same signing certificate. No Actions secret is required.
- Each push to `main` creates a new `debug-latest` pre-release, so there is
  always at most one pre-release in the releases list.
- `versionCode` and `versionName` in debug builds are stamped with
  `<run_number>`, which means locally-built debug APKs always report version
  `1.0`. The installed version must have come from a previous CI build for the
  banner to show; a locally-built sideload will never be "older" than the
  CI build unless the run number is higher.
- Production release behaviour (ADR-010) is unchanged.
