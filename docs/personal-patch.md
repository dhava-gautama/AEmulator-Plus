# The personal patch

This repository is [somerandomusernam2/AEmulator-Plus](https://github.com/somerandomusernam2/AEmulator-Plus) at
`v0.0.0.3.16` plus a deliberately small patch queue. It is **not** an official release and has no support promise.

- Base commit: `7019c78` ("Version 0.0.0.3.16", 2026-11-22)
- Patch version: `0.0.0.3.16-personal.2`, version code `316002`, package `app.aemu.plus`
- Maintainer of this patch: [dhava-gautama](https://github.com/dhava-gautama), not the upstream author
- Upstream triage: [upstream-triage.md](upstream-triage.md) and [upstream-of-upstream.md](upstream-of-upstream.md)

## Commit queue

| Commit | Change | Upstream reference |
| --- | --- | --- |
| `33db9f6` | 3.16 option strings and 18 missing keys in all 58 locales (288 files, strings only; author @power690 kept) | [PR #127](https://github.com/somerandomusernam2/AEmulator-Plus/pull/127) |
| `5f24722` | Network ADB switch persisted per VM and started at boot; dialog and shell broadcast share one path; unit test | [Issue #126](https://github.com/somerandomusernam2/AEmulator-Plus/issues/126) |
| `f6c1146` | Repair the inherited JVM test suite (duplicate class, JUnit imports, stale call signatures, real `org.json`, 2 GiB test heap, missing fixture, test drift) | — |
| `279eeb7` | Readable boot note and minimized boot pill on translucent theme colours (`contentColor = onSurface`) | [Sunset `47853d4`](https://github.com/drel4/AEmulator-Sunset/commit/47853d4) |
| personal branding/docs | Version identity, update endpoint and GitHub link point at this fork; README/AGENT docs; CI workflow | — |

Keep later work in separate, small commits and keep this table current.

Upstream 0.0.0.3.16 does not run `testReleaseUnitTest` successfully: `PldHintsTest.kt.kt` duplicates a class,
`BootClasspathRelocateTest` imports `kotlin.test`, `LgKdzEncryptedTest` predates a `Predicate<String>` parameter, and
`android.jar` only ships throwing `org.json` stubs. The personal patch fixes the test build (and removes the obsolete
`engines/n64-x86_64` leftover) so CI can use the suite as a real gate. If upstream fixes it differently when syncing,
prefer upstream and drop this commit.

## Network ADB behavior

`VmSettings.adb` (stored in each VM's `image.json`) records the switch. `GuestVm.setAdbEnabled()` starts/stops
`AdbServer`, updates the in-memory `adbSaved` flag and writes the setting. `GuestVm.boot()` calls `startSavedAdb()`, so
every boot — including a reboot of the VM inside the same process — restores the switch. A failed bind (no free port) is
stored as off.

The server still binds `0.0.0.0` and picks the first free port in `5555..5585` step 2. **Do not pin the port** without a
deliberate security decision: the server gives anyone on the LAN a root shell in the guest. The upstream maintainer
raised exactly this point on #126. A stable port would make the service easier to discover. If the `adb connect` target
needs to be stable, use mDNS/USB or restrict the network instead.

`app.aemu.SHELL --ez adb true|false` goes through the same persistence path, so scripted testers get the same behavior
as the UI.

## Personal build identity

- `versionCode = 316002`, `versionName = "0.0.0.3.16-personal.2"` in [app/build.gradle.kts](../app/build.gradle.kts).
- `AppUpdateManager` reads releases from `dhava-gautama/AEmulator-Plus` only. Upstream releases are signed with a
  different key and cannot install over this build; offering them would be a broken update.
- The Settings GitHub row points at this repository; the upstream rows are unchanged.
- Release assets intended for the in-app updater must end in `-app.aemu.plus.apk`
  (`UpdateAssetPolicy.matches`). The CI names the release APK `AEmulator-Plus-personal-app.aemu.plus.apk`.

## GitHub Actions

`.github/workflows/android.yml`:

1. JDK 21 (Temurin), Android SDK platform 36, build-tools 36.0.0.
2. Optional signing material from repository secrets (below).
3. `testReleaseUnitTest`, `assembleDebug`, `assembleRelease`.
4. Uploads `app-debug.apk` and `app-release.apk` as workflow artifacts.
5. On a `v*` tag, creates a GitHub Release and attaches the APKs; the release APK is renamed for the updater.

The engine assets are large (~517 MB under `app/src/main/assets/engines`), so expect a cold CI build of roughly
10–25 minutes and an artifact of a few hundred MB. Gradle caches are managed by `gradle/actions/setup-gradle`.

### Release signing

Add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key password |

The workflow writes `keystore.properties` and the decoded keystore to the workspace (both are gitignored). If the
secrets are absent, `assembleRelease` produces an unsigned APK and `assembleDebug` uses the runner's ephemeral debug
key. **Debug APKs from different runners are not update-compatible**; use a stable signing key for anything you install
and later update.

For this personal repository the signing key is kept only in the GitHub secrets and in a local, untracked backup. Losing
it means users must uninstall and reinstall (and lose VM images).

## Local build

The app build does not rebuild native helpers; those prebuilts are inherited. See
[build-source.md](build-source.md). Minimal check:

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
sh gradlew --no-daemon testReleaseUnitTest assembleDebug
```

A full release build with the bundled engines needs several GB of free disk and a few GB of RAM. On the maintenance PC,
prefer pushing a branch and letting CI build.

## Syncing with upstream

This is a patch queue, not a permanent divergence. When upstream publishes a new version:

1. Fetch upstream `main`, create a branch and merge/rebase it onto this history.
2. Drop any patch that upstream has already fixed (especially the ADB persistence change).
3. Resolve localization conflicts in favour of upstream if #127 was merged there; otherwise keep the locale files.
4. Re-read `docs/upstream-triage.md`, update it, and bump the patch version:
   `versionName = "<upstream>-personal.<n>"`, `versionCode` must increase.
5. Push the branch; let CI run the unit tests and build both APKs. Only tag after CI is green.
6. Tag `v<versionName>` and publish the release asset named `AEmulator-Plus-personal-app.aemu.plus.apk`.

Never force-push `main` after publishing a release: the updater compares version names, and a rewritten release can
confuse installed clients.

## Known limitations inherited from upstream

- Only `arm64-v8a` hosts; Android 8.0+.
- The license audit is incomplete for bundled prebuilts ([license-audit.md](license-audit.md)).
- Many firmware-specific boot failures remain upstream issues; this patch does not touch the engine.
- The app still uses `targetSdk = 28` deliberately (guest binaries execute from the app data directory).
