# AGENT.md — working on the AEmulator Plus personal patch

This file is for AI coding agents (and humans) who touch this repository. Read it before changing anything. The
project is a **personal patch queue** on top of an Android emulator; a wrong change can make firmware unbootable in
ways unit tests cannot detect.

## 1. What this repository is

- Base: `somerandomusernam2/AEmulator-Plus` at `v0.0.0.3.16` (commit `7019c78`), itself a fork of
  `drel4/AEmulator-Sunset` and `uxazu/AEmulator`.
- This fork: `dhava-gautama/AEmulator-Plus`, an **unofficial personal patch**, built by GitHub Actions.
- The emulator engine (patched QEMU, guest shims, HALS, prebuilt `.so`/binaries) is inherited from upstream. This fork
  changes host-side Kotlin/resources/docs/CI, not the engine.
- License: GPL-3.0. Keep the upstream copyright/attribution and dated modification notices intact.

The maintainer's PC is weak. **CI is the build machine**: push a branch and use the `Android CI` workflow instead of
running a full local build unless you are debugging Gradle/CI itself.

## 2. Read these first

1. [README.md](README.md) — what the build is, how to install, the signature caveat.
2. [docs/personal-patch.md](docs/personal-patch.md) — the current commit queue, ADB behavior, signing, upstream sync.
3. [docs/upstream-triage.md](docs/upstream-triage.md) — which upstream issues/PRs are worth touching and why others are
   not.
4. [docs/build-source.md](docs/build-source.md) — the upstream toolchain and native/helper build details.
5. [NOTICE.md](NOTICE.md) and [docs/license-audit.md](docs/license-audit.md) — provenance and the licensing caveat.

## 3. Ground rules

- **Keep the patch small.** One concern per commit; no drive-by refactors, formatting sweeps or dependency upgrades.
- **Do not replace engine prebuilts** (`app/src/main/assets/engines/**`, `app/src/main/jniLibs/**`, `qemu-x86/**`,
  `native/**` binaries) unless you have the matching source, the exact toolchain and a guest test rig. A rebuilt binary
  with no test is a regression risk.
- **Do not commit** `keystore.properties`, `*.jks`, `*.keystore`, APKs, ROMs/firmware, `release-apks/`, build output,
  IDE files or machine-specific paths. They are gitignored; keep it that way.
- **Preserve authorship** when importing an upstream PR. Use `--author="Name <id+login@users.noreply.github.com>"` and
  put the upstream PR/issue link in the commit body.
- **Document every change** in `docs/personal-patch.md` and, if it came from an issue/PR, in
  `docs/upstream-triage.md`.
- **Never claim device testing you did not do.** If a change needs a booted firmware, say so; do not merge it into the
  patch queue without a tester.
- Upstream issue threads contain hostile/off-topic comments. Do not copy them into this repository or respond to them.
- No secrets, tokens, personal data or downloader credentials in the repo or logs.

## 4. Repository map

| Path | What lives there |
| --- | --- |
| `app/src/main/java/app/aemu/` | host app: `ui/` (Compose UI and activities), `core/` (VM, image store, engines, bridges), `importer/`, `catalog/`, `update/`, `archive/`, `netdebug/` |
| `app/src/main/assets/engines/` | per-API engine trees + guest-side libraries/JARs (large, content-addressed into the APK by `packEngines`) |
| `app/src/main/jniLibs/arm64-v8a/` | host JNI libraries (QEMU runner, GL bridge, archive) |
| `app/src/main/res/` | strings in 58 locales, themes, XML resources |
| `app/src/test/java/` | JVM unit tests (`testReleaseUnitTest`) |
| `native/` | helper source and build scripts (not part of the Gradle app build) |
| `qemu-x86/`, `tools/` | engine/tooling sources and helpers |
| `docs/` | upstream docs, translation READMEs, this fork's patch docs |
| `.github/workflows/android.yml` | CI build/release |
| `gradle/`, `gradlew`, `build.gradle.kts`, `app/build.gradle.kts` | Gradle 8.14.2, AGP 8.13.2, Kotlin 2.2.21 |

Key host classes for common work:

- `core/GuestImage.kt` — `GuestImage` + `VmSettings` and their JSON (this is the per-VM `image.json`).
- `core/ImageStore.kt` — list/get/save/update machine metadata.
- `core/GuestVm.kt` — one running machine, boot sequence, ADB server, settings snapshot.
- `core/AdbServer.kt` — host-side ADB wire protocol (shell/sync/reboot) over TCP.
- `ui/VmActivity.kt` — machine window, menu dialogs (ADB, shell, settings), broadcast test hooks.
- `ui/AppSettingsActivity.kt` — app-wide settings, links.
- `update/AppUpdateManager.kt` — release check/download; points at this fork.

## 5. Build and test

Tested toolchain: **JDK 21**, **Android SDK platform 36**, **build-tools 36.0.0**, Gradle wrapper **8.14.2**. Bytecode
targets Java 17. `minSdk 26`, `targetSdk 28` (deliberate: guest binaries run from the app data directory), only
`arm64-v8a`.

### CI (default)

```bash
git switch -c fix/<topic>
# ... edit ...
git add -A && git commit
git push -u origin fix/<topic>
gh run watch --repo dhava-gautama/AEmulator-Plus
```

The workflow runs `testReleaseUnitTest`, `assembleDebug` and `assembleRelease`, and uploads both APKs. A `v*` tag
creates a GitHub Release with an updater-compatible asset name (`...-app.aemu.plus.apk`).

### Local

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk

sh gradlew --no-daemon testReleaseUnitTest          # JVM tests, fastest signal
sh gradlew --no-daemon assembleDebug                # installable APK
sh gradlew --no-daemon assembleRelease              # unsigned unless keystore.properties exists
sh gradlew --no-daemon copyReleaseApks              # release-apks/<version>/
```

Notes/traps:

- `packEngines` regenerates `build/generated/engineAssets` on every build: ~517 MB of assets hashed and deduplicated.
  A cold build needs several GB of disk and time; do not "optimize" this task casually.
- Unit tests run against the Android stub plus real `org.json`; add tests for pure Kotlin logic (settings JSON, parsing,
  policy helpers), not for anything that needs a Surface/guest.
- There is no emulator instrumentation suite and no clone application ID (`app.aemu.plus` only).
- `./gradlew lint` is not part of CI; `lint.abortOnError = false` upstream. Do not make lint failures block the build
  without fixing them.
- Release signing is opt-in through an untracked `keystore.properties`; CI uses repository secrets. Never print secret
  values in logs.

### Manual checks a unit test cannot replace

If a change is guest-visible (engine, importer, guest properties, boot timing), get a tester to boot at least one
firmware and attach `host.log`/`logcat`. Do not mark such work "done" from a green compile.

## 6. Adding or refreshing a patch

1. Search upstream issues/PRs and read the discussion, not just the title. Prefer a linked reproduction/log.
2. Decide whether the change is host-side and testable here. If it needs a specific firmware or a rebuilt engine
   prebuilt, it is out of scope for this patch queue (document it in `docs/upstream-triage.md` instead).
3. Implement the smallest change. Add a unit test when the logic is pure.
4. Add a dated `/* Personal patch, YYYY-MM-DD: ... GPL-3.0 ... */` header to modified source files that already carry
   such headers (or a short comment where the file has none). Keep existing upstream notices.
5. Update `docs/personal-patch.md` (commit queue table) and `docs/upstream-triage.md` (decision).
6. Bump the version if the change is user-visible:
   - `versionName = "<upstream version>-personal.<n>"`, e.g. `0.0.0.3.16-personal.2`
   - `versionCode` must strictly increase (currently `316001`; use e.g. `316002`).
7. Run the unit tests locally if the SDK is available, otherwise push a branch and let CI prove it.
8. Tag only after CI is green: `git tag v<versionName> && git push origin v<versionName>`.

## 7. Coding conventions

- Kotlin, 4-space indent, no wildcard imports in new code (the file may already have some). Match the surrounding style.
- User-facing strings go to `app/src/main/res/values/strings.xml`; when adding a key, add it to all 58 locale files or
  at least the English one and note the gap. The translation completeness patch (PR #127) exists because missing keys
  silently fall back to English.
- Keep the upstream code comments (many are Russian) and add new comments in English.
- Settings that affect a booted guest must say **when** they apply (next full boot vs live) in the KDoc, matching
  `VmSettings` style.
- Persist per-VM state in `VmSettings`/`image.json` through `ImageStore.update`, not in `SharedPreferences` (each VM is
  a separate process and `image.json` is the source of truth). App-wide state belongs in `AppPrefs`.
- Do not log secrets; `AdbServer` logs addresses/ports, which is fine, but not shell contents by default.
- When in doubt, read `docs/personal-patch.md` and the upstream issue before writing code.

## 8. Release and update channel

- In-app updates only consult `dhava-gautama/AEmulator-Plus` releases. Do not point them upstream: a different signing
  key cannot install over this build.
- Asset names must end with `-app.aemu.plus.apk` for `UpdateAssetPolicy.matches`.
- Version comparison is numeric per dot segment (`0.0.0.3.16-personal.2` > `...personal.1`), so keep the
  `-personal.N` suffix format.
- Published releases are effectively immutable for installed clients; bump, do not rewrite tags.

## 9. When upstream releases a new version

Follow [docs/personal-patch.md § Syncing with upstream](docs/personal-patch.md#syncing-with-upstream). In short:
merge upstream, drop fixes upstream already made (especially ADB persistence), resolve locale conflicts in upstream's
favor if #127 landed, update the triage/patch docs, bump `-personal.N`, let CI build, then tag.

## 10. Definition of done

- [ ] Change is minimal and attributable.
- [ ] `testReleaseUnitTest` passes (CI or local).
- [ ] `assembleDebug`/`assembleRelease` pass (CI or local).
- [ ] Docs updated: `docs/personal-patch.md`, `docs/upstream-triage.md`, README if user-visible.
- [ ] Version bumped if needed; release asset name is updater-compatible.
- [ ] No binaries, keys, ROMs, build outputs or machine paths committed.
- [ ] If guest-visible: a real boot test was performed and recorded; otherwise the limitation is stated.
