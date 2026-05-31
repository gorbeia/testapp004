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

The shared debug keystore (`keystores/debug.keystore`) is committed to the
repository. CI signs debug APKs with it directly — no secret required.

**One-time setup (per developer):**

```bash
# Replace the repo keystore with your own so sideloaded and CI builds match
cp ~/.android/debug.keystore keystores/debug.keystore
git add keystores/debug.keystore
git commit -m "chore: add shared debug keystore"
```

The default Android debug keystore credentials (`android` / `androiddebugkey`
/ `android`) are assumed. If yours were generated with different credentials,
update the `signingConfigs.debug` block in `app/build.gradle.kts`.

**How it works:**

1. Every push to `main` triggers `debug-prerelease.yml`.
2. The debug APK is built with `versionName = "1.0.<run_number>"`, signed
   with `keystores/debug.keystore` via the Gradle signing config.
3. The APK is uploaded to a `debug-latest` pre-release (the previous one is
   deleted first).
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
