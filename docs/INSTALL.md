# Installing testapp004 on Your Android Device

## Requirements

- Android 7.0 (API 24) or higher
- USB debugging enabled **or** a GitHub account with repo access

---

## Option 1 — Download the debug APK from GitHub Actions (easiest)

Use this for testing. No signing setup required.

1. Go to the repository on GitHub.
2. Click **Actions** → select the latest **CI** workflow run on your branch.
3. Scroll to **Artifacts** at the bottom of the run page.
4. Download **debug-apk** and unzip it — you get `app-debug.apk`.
5. Transfer the file to your phone (email, Google Drive, USB cable, etc.).
6. On your phone, open **Settings → Apps → Special app access → Install unknown apps**, then allow your file manager or browser to install APKs.
7. Tap the APK file and follow the prompts.

> Debug APK artifacts are kept for **14 days** per run. Grab them before they expire.

---

## Option 2 — Install a signed release APK via GitHub Releases (recommended for sharing)

Signed release APKs are published automatically when you push a version tag (e.g. `v1.0.0`).

### One-time setup: create a signing keystore

```bash
keytool -genkeypair \
  -v \
  -keystore release.keystore \
  -alias testapp004 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Keep `release.keystore` **out of version control** (it's already in `.gitignore`).

### Add the keystore as GitHub secrets

1. Base64-encode the keystore:
   ```bash
   base64 -w 0 release.keystore > release.keystore.b64
   ```
2. In the repository on GitHub go to **Settings → Secrets and variables → Actions → New repository secret** and add:

   | Secret name          | Value                                  |
   |----------------------|----------------------------------------|
   | `SIGNING_KEY_BASE64` | Contents of `release.keystore.b64`    |
   | `KEY_ALIAS`          | The alias you chose (e.g. `testapp004`)|
   | `KEY_STORE_PASSWORD` | Keystore password                      |
   | `KEY_PASSWORD`       | Key password                           |

### Publish a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The **Release** workflow builds, signs, and uploads `app-release-signed.apk` to a new GitHub Release. Download it from the **Releases** page and install it as in step 5-7 above.

---

## Option 3 — Build and install locally via ADB

```bash
# Connect phone via USB with USB debugging on
adb devices                        # confirm device is listed
./gradlew installDebug             # build + install in one step
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "App not installed" | Enable **Install unknown apps** for the app you used to open the APK |
| "Parse error" | The APK may be corrupted; re-download it |
| Device not shown by `adb devices` | Toggle USB debugging off/on; try a different cable |
| Signing workflow fails | Verify all four secrets are set correctly in repository settings |
