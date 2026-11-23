# Upstream of upstream — audit, 2026-11-23

The personal fork is based on [somerandomusernam2/AEmulator-Plus](https://github.com/somerandomusernam2/AEmulator-Plus)
0.0.0.3.16. That project is a fork of [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset), which is a
fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). This document checks both older repositories for patches
that are missing from Plus and safe to take here.

Method: `git fetch` both `main` branches, compare with `git rev-list main..<upstream>/main`, read every commit that is
only in the older repo, and check whether its files/behaviour already exist in this tree. Test/compile results were then
checked with CI.

| Repo | `main` at audit | Latest release | Commits only there |
| --- | --- | --- | --- |
| `drel4/AEmulator-Sunset` | `2b99b5a` (2026-10-08) | `v0.0.0.3-sunset.38` (2026-10-07) | 13 (Plus is 72 ahead) |
| `uxazu/AEmulator` | `3b97ed5` (2026-10-18) | `0.0.0.3` (pre-release, 2026-10-18) | 2 (Plus is 109 ahead) |

Neither repository has open issues that apply: Sunset has issues disabled; uxazu's open issues are firmware/engine
reports (`#52`, `#69`, `#74`, …) without patches. Sunset has no open pull requests.

## Sunset — the 13 commits

| Commit | Change | In Plus already? | Decision |
| --- | --- | --- | --- |
| `a4a2c00` | Compact library badges, FlowRow, header spacing | Yes: `ui/LibraryLabels.kt`, `FlowRow` in `MainActivity` | Nothing to take |
| `41dc8df` | CM11 first-boot IME guard | ROM build recipe (`rom/cm11`), not the app | Not an app patch |
| `f612b85`, `cf7e2df` | CM11 verification records | Documentation for the CM11 guest package | Not an app patch |
| `717522f` | Wait for guest property completion (first-boot IME race) | Yes: `native/guestshim/property-client.h` is identical and the bundled shim is newer (56 KiB vs 20 KiB) | Nothing to take |
| `566f299` | `AGENTS.md` contributor workflow | Superseded by this fork's [AGENT.md](../AGENT.md) | Not copied |
| `255bdaf` | Export notes, restore CM11 media codec config | Yes: `OneTimeVmNote`, export note UI and archive support | Nothing to take |
| `767afc7` | Restore `/etc -> /system/etc` alias | Yes: `GuestRootAliases.ensureEtc` is called by `TreeFixer` | Nothing to take |
| `6f16610` | Background ROM downloads, progress, nested factory archives | Yes: `catalog/RomDownloadService`, `importer/FirmwareContainers` | Nothing to take |
| `165d9a4` | Host battery following, catalog links to browser | Yes: `HostBatteryBridge/State`, `ui/BrowserLinks` | Nothing to take |
| `7eac902` | Persistent **ROM card note** (separate from the one-time note) | **No**: Plus only has the one-time boot note | Not taken; see below |
| `47853d4` | Readable note/loading cards on translucent custom colours | **No** for two `Surface`s | **Applied** in `personal.2` |
| `2b99b5a` | Experimental **Universal Sunset Extended** compatibility profile | **No** | Not taken; see below |

### Applied: `47853d4` contrast fix

Two `Surface`s in `ui/VmActivity.kt` use `surfaceContainerHigh.copy(alpha = …)`. A translucent custom colour does not
always resolve a content colour, so the boot note and the minimized boot pill could be unreadable in some themes. The
fix sets `contentColor = MaterialTheme.colorScheme.onSurface` on both. Host-side, no guest behaviour, no binary change.

### Not taken: `7eac902` persistent ROM card note

This is a feature (an author-supplied note stored in the portable profile and shown on the library card), not a bug
fix. Plus replaced the card UI with the 0.3 grid/3D library and currently exposes only the one-time boot note; taking
the Sunset implementation would need a second note model, archive migration, a new card widget and UI for both library
modes. It is a reasonable future patch but does not belong in a fix-only release.

### Not taken: `2b99b5a` Universal Sunset Extended

An **experimental, opt-in** compatibility profile that:

- keeps firmware search-path order and appends guest-contained command/library directories,
- honours service-local `setenv` from parsed init files, with quoting/escaping, and transports values through
  `DHD_ENV_*` instead of the host loader,
- adds an engine picker behind the experimental-feature gate.

The commit's own release record says **no on-device boot/graphics/hardware validation was performed**. It changes
guest-visible environment handling on the KK engine, adds a `GuestService.environment` field to `image.json`, and its
UI predates Plus's rewritten settings sheet and 58-locale strings. Applying it would require a careful port and a real
firmware test, which this patch queue cannot provide. It remains a candidate for a separate opt-in branch.

### Not taken: CM11 recipe commits

`41dc8df`, `f612b85` and `cf7e2df` touch `rom/cm11/*` only. They build/verify a CM11 guest image; they do not change the
APK. Skipped.

## uxazu — the 2 commits

| Commit | Change | Decision |
| --- | --- | --- |
| `3b97ed5` | 0.0.0.3 feature drop (new engine, importers, HAL helpers, `tools/guesttest*`) | Plus already contains the app/native files and the guesttest harness. The only files absent here are `CHANGELOG.md`, its translations, and camera HAL3 sources (`camera_hal3.c`, `camera_tags_29.h`, `camera_tags_aosp.h`, `camera_gb_none.c`, `build3.sh`). Plus ships an older HAL1 camera bridge; taking HAL3 would mean rebuilding and testing a native prebuilt. Not taken. |
| `38d9a00` | README credit for the original SENSES emulator | Credit already appears in this fork's README/NOTICE chain. No action. |

### uxazu open pull requests

| PR | State | Decision |
| --- | --- | --- |
| [#53](https://github.com/uxazu/AEmulator/pull/53) | open, mergeable | Adds 16/24 GB to the RAM slider. Plus already offers up to 12 GB plus a custom amount; the higher presets can exceed the host's RAM and were not tested. Not taken. |
| [#48](https://github.com/uxazu/AEmulator/pull/48) | open, mergeable | "Match the phone" resolution, fullscreen guest, host battery, draggable menu, extra RAM steps, sdcard mount points, named folders. Plus already has equivalents for the resolution, battery, menu and sdcard work (its implementation differs and is newer). Also reports a 16 KB-page GSI boot failure with no fix. Not taken. |
| [#29](https://github.com/uxazu/AEmulator/pull/29) | open, mergeable | A generic `SECURITY.md` template with placeholder versions (`5.1.x`, `4.0.x`) that do not describe this app. Not applicable. |

## Re-checking later

```bash
git fetch --no-tags sunset main uxazu main
git log --oneline main..sunset/main
git log --oneline main..uxazu/main
```

Before taking a commit, check whether its files already exist here (`git cat-file -e main:<path>`) and whether the
feature is visible in the current UI. For native/engine changes, do not take the source without building and testing the
corresponding prebuilt.
