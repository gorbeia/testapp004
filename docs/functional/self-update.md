# Self-update

The app checks for a newer version on startup and shows a dismissible banner
if one is available. Tapping **Update** downloads the APK and fires the
system installer.

---

## Update channels

| Build type | GitHub endpoint | Who publishes |
|------------|----------------|---------------|
| Release (`assembleRelease`) | `/releases/latest` | Tag `v*.*.*` → `release.yml` CI |
| Debug (`assembleDebug`) | `/releases/tags/debug-latest` | Push to `main` → `debug-prerelease.yml` CI |

---

## How the check works

1. `UpdateViewModel.init` calls `GitHubUpdateRepository.checkForUpdate(currentVersionName)`.
2. The repository hits the appropriate GitHub Releases endpoint (see table above).
3. The `tag_name` is stripped of its `v` prefix and compared semantically
   against `BuildConfig.VERSION_NAME`.
4. If the remote version is strictly newer, an `AppRelease` (version name +
   APK download URL) is returned; otherwise `null`.
5. A non-null result sets `UpdateUiState.updateAvailable`, which makes the
   `UpdateBanner` composable visible.

---

## Publishing a production release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Push a `v<major>.<minor>` tag (e.g. `git tag v1.1 && git push origin v1.1`).
3. `release.yml` runs automatically: tests → builds signed release APK →
   creates GitHub Release with the APK attached.
4. Any device with an older release build will show the banner on next launch.

---

## Setting up the debug update channel

The debug CI workflow re-signs the APK with the developer's own debug keystore
so Android accepts the install over the sideloaded debug build.

**One-time setup:**

```bash
# Export your local debug keystore as a base64 string
base64 -w 0 ~/.android/debug.keystore
```

Add the output as an Actions secret named `DEBUG_KEYSTORE_BASE64` in the
repository settings (Settings → Secrets and variables → Actions).

The default Android debug keystore credentials (`android` / `androiddebugkey`
/ `android`) are assumed. If yours differ, update the `--ks-pass`, `--key-pass`,
and `--ks-key-alias` arguments in `debug-prerelease.yml`.

**How it works after setup:**

1. Every push to `main` triggers `debug-prerelease.yml`.
2. The debug APK is built with `versionName = "1.0.<run_number>"`.
3. It is re-signed with your debug keystore and uploaded to a `debug-latest`
   pre-release (the previous one is deleted first).
4. The debug app on your device checks `/releases/tags/debug-latest` on
   startup and shows the banner if the build number is higher than installed.

---

## Component map

| Component | File |
|-----------|------|
| Update check + version comparison | `data/GitHubUpdateRepository.kt` |
| State + download logic | `viewmodel/UpdateViewModel.kt` |
| Banner UI | `navigation/AppNavigation.kt` (`UpdateBanner`) |
| DI wiring | `di/NetworkModule.kt` |
| Release CI | `.github/workflows/release.yml` |
| Debug pre-release CI | `.github/workflows/debug-prerelease.yml` |
