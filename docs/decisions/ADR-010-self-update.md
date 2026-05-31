# ADR-010 — Debug APK self-update via GitHub Releases

**Date:** 2026-05-31
**Status:** Accepted

---

## Context

Distributing updated debug/internal builds requires a manual `adb install` step.
To reduce friction during iterative development and testing, the app should be
able to detect that a newer release is available and install it without leaving
the device.

The project already has a `release.yml` GitHub Actions workflow that publishes
signed APKs to GitHub Releases on every `v*.*.*` tag.

---

## Decision

Implement an in-app self-update mechanism backed by the GitHub Releases API:

1. On startup the app calls `GET /repos/gorbeia/testapp004/releases/latest`.
2. The `tag_name` from the response (e.g. `v1.2`) is compared semantically
   against `BuildConfig.VERSION_NAME`.
3. If the remote version is newer, a dismissible banner appears at the top of
   every screen.
4. Tapping **Update** downloads the APK asset into the app's cache directory and
   fires an `ACTION_VIEW` install intent via `FileProvider`.

**Libraries added:**
- `com.squareup.okhttp3:okhttp:4.12.0` — HTTP client for the API call and APK download.

**Permissions added:**
- `android.permission.INTERNET` — required for all network access.
- `android.permission.REQUEST_INSTALL_PACKAGES` — required to install APKs
  from outside the Play Store (Android 8.0+).

**New components:**

| Component | Role |
|-----------|------|
| `UpdateRepository` / `GitHubUpdateRepository` | Fetches and parses the latest release; compares versions |
| `NetworkModule` | Hilt module providing `OkHttpClient` singleton and binding `UpdateRepository` |
| `UpdateViewModel` | Checks for update on init, drives download, emits install intent via `SharedFlow` |
| `UpdateBanner` (private composable in `AppNavigation`) | Non-intrusive card banner; hides when dismissed or when no update |

---

## Consequences

- The app now requires INTERNET permission (no network calls existed before).
- Users on Android 8.0+ will be prompted to allow "install unknown apps" from
  this app the first time they tap **Update**; this is handled by the OS.
- The `FileProvider` authority is `${applicationId}.provider`; the shared path
  is the cache directory (`cache/downloads/`).
- `BuildConfig` generation is explicitly enabled (`buildConfig = true`) since
  AGP 8.x disables it by default.
- Version bumps (`versionCode` + `versionName` in `app/build.gradle.kts`) and a
  new tag (`v<major>.<minor>`) are required before each release for the check to
  trigger.
