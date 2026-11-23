<div align="center">

<img src="docs/assets/logo.png" width="128" alt="AEmulator Plus logo"/>

# AEmulator Plus — Personal Patch

**An unofficial, personal patch build of [AEmulator Plus](https://github.com/somerandomusernam2/AEmulator-Plus).**
Not affiliated with, endorsed by, or supported by the upstream author. Built by GitHub Actions because the
maintenance PC is slow. Upstream features and credits are kept below.

[![Android CI](https://github.com/dhava-gautama/AEmulator-Plus/actions/workflows/android.yml/badge.svg)](https://github.com/dhava-gautama/AEmulator-Plus/actions/workflows/android.yml)

[🇬🇧 English](README.md) · [Upstream translations (may describe upstream, not this patch)](docs/i18n/)

</div>

---

> [!WARNING]
> **This is a personal patch, not an official AEmulator Plus release.**
> It is published so its few changes and reproducible CI builds can be inspected. Do not report problems with this
> build to the upstream author. Ask upstream first whether a bug also happens on an official release. The APK is signed
> with a personal key, so it **cannot update** an installed upstream/official build: uninstall that first (the VM images
> in the app's private storage are deleted with it, so export anything you need).
>
> Source and binaries are provided under GPL-3.0, without warranty. Android, vendor skins and firmware belong to their
> owners. See [NOTICE.md](NOTICE.md) and the [upstream licensing audit](docs/license-audit.md).

## What this patch is

This repository is a patch queue on top of upstream **0.0.0.3.16**, not a new emulator. The upstream app boots a real
Android 1.0–17 system image from a firmware file with a patched QEMU, a host-side binder/audio/GPU bridge and its own
importer. That engine is unchanged here.

The personal patch currently contains:

1. **Network ADB survives a restart** — the VM menu's *ADB over network* switch is stored per VM and the server starts
   again when the machine boots. Both the dialog switch and the `app.aemu.SHELL --ez adb` broadcast use the same path.
   The listen port stays dynamic (first free port in 5555–5585) on purpose: a fixed root shell on `0.0.0.0` is easier
   to find on a shared LAN (this is also the upstream maintainer's security argument).
   Fixes [upstream issue #126](https://github.com/somerandomusernam2/AEmulator-Plus/issues/126).
2. **Complete 3.16 translations** — the 38 new option strings and 18 missing keys are added to all 58 locales from
   [upstream PR #127](https://github.com/somerandomusernam2/AEmulator-Plus/pull/127) by @power690, with authorship kept
   in the commit.
3. **Personal build identity** — `0.0.0.3.16-personal.1` / version code `316001`, update checks and the GitHub link point
   at this fork instead of upstream, and CI builds both debug and release APKs.
4. **GitHub Actions builds** — [.github/workflows/android.yml](.github/workflows/android.yml) runs the unit tests and
   builds debug + release APKs on every push and tag. Nothing is compiled on the maintenance PC.
5. **A test suite that actually runs** — upstream 0.0.0.3.16 cannot execute `testReleaseUnitTest` (a duplicate test
   class, a missing `kotlin.test` dependency, stale call signatures, `android.jar`'s `org.json` stubs, a too-small test
   heap and some test drift). Those are repaired here, so CI is a real signal rather than a build-only check.

See [docs/personal-patch.md](docs/personal-patch.md) for the exact diff and the
[upstream triage](docs/upstream-triage.md) for which open issues/PRs were considered (and why most engine bugs are not
patched here).

## Download and install

1. Open [Releases](https://github.com/dhava-gautama/AEmulator-Plus/releases) and download the newest
   `AEmulator-Plus-*-app.aemu.plus.apk`.
2. Uninstall any official AEmulator Plus build first (different signing key, same package `app.aemu.plus`).
3. Install the APK, open it, and import a firmware you own. Import takes a few minutes; the first boot is slower.

Requirements are the upstream ones: Android 8.0+ on a 64-bit ARM phone (arm64-v8a), 1–3 GB free per firmware, and
ideally a Snapdragon SoC (Mali GPUs have known issues).

## Build

The tested toolchain is JDK 21, Android SDK platform 36, build-tools 36.0.0 and the checked-in Gradle 8.14.2 wrapper.
Kotlin/Java bytecode targets Java 17. AI agents should read [AGENT.md](AGENT.md) before changing anything.

### GitHub Actions (recommended here)

Push to `main`, open a pull request, or run **Actions → Android CI → Run workflow**. The workflow runs
`testReleaseUnitTest`, `assembleDebug` and `assembleRelease`, then uploads the APKs as artifacts. Pushing a tag such as
`v0.0.0.3.16-personal.2` also publishes a GitHub Release with an APK asset named
`AEmulator-Plus-personal-app.aemu.plus.apk` (the in-app updater only accepts names ending in `-app.aemu.plus.apk`).

Optional signing: add the repository secrets `SIGNING_KEYSTORE_BASE64` (base64 of a `.jks`),
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`. Without them the release APK is unsigned (the
debug APK uses the runner's throwaway debug key). See [docs/personal-patch.md](docs/personal-patch.md#release-signing).

### Locally

```bash
git clone https://github.com/dhava-gautama/AEmulator-Plus.git
cd AEmulator-Plus
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
sh gradlew --no-daemon testReleaseUnitTest assembleDebug
```

`./gradlew copyReleaseApks` copies the release APK to `release-apks/<version>/`. Release signing uses an untracked
`keystore.properties`; without it the release APK is unsigned. Upstream native/helper rebuilds (`native/*`) are **not**
part of the app build and are unchanged by this patch — see [docs/build-source.md](docs/build-source.md).

## Upstream and credits

This patch would not exist without the upstream work. Please support and report genuine app bugs upstream:

- Personal patch: [dhava-gautama/AEmulator-Plus](https://github.com/dhava-gautama/AEmulator-Plus)
- Upstream (base for this patch): [somerandomusernam2/AEmulator-Plus](https://github.com/somerandomusernam2/AEmulator-Plus)
- Upstream's base: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset) and
  [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- Original HTC Desire HD / One M7 engine author: [t.me/istratiit_ech](https://t.me/istratiit_ech)

Upstream contributors keep their credit: @RYuhMine (ZTE MMI images, Wear SDK fixes, `ro.hardware`), @afonsocarolas
(emulated SIM/Wi-Fi, Portuguese, `.sin`/`.ftf`, camera, 3D UI/skins), @felipevidal640-hue (microphone), @power690
(battery, resolution, boot card; also the translation completion used here), @TwrpGuy (ROM catalog), and RVM
(Mali/Samsung fixes).

## License

GPL-3.0; see [LICENSE](LICENSE), [NOTICE.md](NOTICE.md) and the [dated modification notices](docs/license-audit.md).
The GPL label is not a certification that every bundled prebuilt has complete matching source; that caveat is inherited
unchanged from upstream.
