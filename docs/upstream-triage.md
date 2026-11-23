# Upstream triage — 2026-11-23

This is a snapshot of the open issues and pull requests on
[somerandomusernam2/AEmulator-Plus](https://github.com/somerandomusernam2/AEmulator-Plus) when this personal patch was
started, and what was done with each. The goal is a **small, reviewable patch queue**, not a competing fork.

## Pull requests

| PR | State | What it changes | Decision |
| --- | --- | --- | --- |
| [#127](https://github.com/somerandomusernam2/AEmulator-Plus/pull/127) | open, mergeable | Adds the 38 new 3.16 option strings and 18 missing keys (Magisk, RAM type, S-Pen, boot warnings) to all 58 locales; 288 files, +3592/−0 | **Taken** in `33db9f6`, authorship kept for @power690. Strings-only, no behavior change. |
| [#119](https://github.com/somerandomusernam2/AEmulator-Plus/pull/119) | open, mergeable | Fixes Wi-Fi `getCurrentNetwork` null crash, toggle sync and status-bar bars; adds multiple emulated networks | **Not taken.** 79 files, +1035/−297, including a rebuilt `aemu-wifi.jar`; the author tested Android 10 and 6.0.1 only, explicitly not Android 11+ options or unit tests. Needs guest testing this PC cannot do. |
| [#120](https://github.com/somerandomusernam2/AEmulator-Plus/pull/120) | open, conflicting | Follow-up Wi-Fi fix: primary link, signal refresh, `ParceledListSlice`/empty-list returns, rebuilt jar | **Not taken.** Correct direction, but conflicts with `main` and ships another prebuilt jar that cannot be verified here. Revisit with a guest test rig. |
| [#52](https://github.com/somerandomusernam2/AEmulator-Plus/pull/52) | open, conflicting | "Ccr 1fa6ad29 cdmpvk": 117 files, +2369/−544, no description | **Not taken.** Looks like an unreviewed generated branch; no scope or rationale to evaluate. |

## Open issues

### Patched

| Issue | Problem | Decision |
| --- | --- | --- |
| [#126](https://github.com/somerandomusernam2/AEmulator-Plus/issues/126) | Network ADB switch is in-memory only; every VM/app restart needs a manual toggle, and the port is dynamic | **Fixed** in `5f24722`: the switch is stored in `VmSettings` (`image.json`), auto-started at boot, and both the dialog and the `app.aemu.SHELL --ez adb` broadcast persist it. The maintainer argued against a fixed port (LAN-accessible root shell), so the dynamic port is kept. |

### Not patched here

| Issues | Category | Why not in this patch |
| --- | --- | --- |
| [#125](https://github.com/somerandomusernam2/AEmulator-Plus/issues/125), [#122](https://github.com/somerandomusernam2/AEmulator-Plus/issues/122), [#121](https://github.com/somerandomusernam2/AEmulator-Plus/issues/121), [#117](https://github.com/somerandomusernam2/AEmulator-Plus/issues/117), [#114](https://github.com/somerandomusernam2/AEmulator-Plus/issues/114) | Specific firmware fails to boot / zygote exits 137 | Engine and firmware-specific. Needs the exact ROM, a real-device log and iterative guest testing; cannot be validated by compilation or unit tests. Report upstream with a full log zip. |
| [#92](https://github.com/somerandomusernam2/AEmulator-Plus/issues/92), [#106](https://github.com/somerandomusernam2/AEmulator-Plus/issues/106), [#101](https://github.com/somerandomusernam2/AEmulator-Plus/issues/101), [#95](https://github.com/somerandomusernam2/AEmulator-Plus/issues/95) | Black screen on 4.4-, slow Android 17 boot, Wi-Fi/radio on 17, TouchWiz audio dying | Same: engine/bridge behavior, not host UI. #101 may overlap PR #119/#120, which were not taken. |
| [#124](https://github.com/somerandomusernam2/AEmulator-Plus/issues/124), [#105](https://github.com/somerandomusernam2/AEmulator-Plus/issues/105), [#103](https://github.com/somerandomusernam2/AEmulator-Plus/issues/103) | One APK crashes, old Unity APKs fail to install, guest WebView crashes | Needs the APK/firmware and guest logs; could be a generic MediaCodec/WebView limitation rather than a host bug. |
| [#123](https://github.com/somerandomusernam2/AEmulator-Plus/issues/123) | LSPosed install fails | Magisk/LSPosed integration is version- and firmware-sensitive; no reproducible steps or logs in the report. |
| [#97](https://github.com/somerandomusernam2/AEmulator-Plus/issues/97) | VPN app throws `IllegalStateException` | Guest VPN needs a `VpnService`/TUN path the emulator does not expose; a host patch would be a feature, not a bug fix. |
| [#109](https://github.com/somerandomusernam2/AEmulator-Plus/issues/109) | Simulated incoming call does nothing on some ROMs | Upstream confirmed the call UI is AOSP/Samsung-specific; the reporter's ROM link is needed. Firmware-specific. |
| [#108](https://github.com/somerandomusernam2/AEmulator-Plus/issues/108), [#96](https://github.com/somerandomusernam2/AEmulator-Plus/issues/96), [#85](https://github.com/somerandomusernam2/AEmulator-Plus/issues/85) | UI suggestions: hiding tab bar fully, custom ROM download folder/copy link, expose VM files to MT Manager | Feature/UX requests. Out of scope for a patch whose value is a reproducible build and a tiny bug-fix queue; better discussed upstream to avoid forking UI behavior. |

## Rules used for inclusion

1. The change must be verifiable without booting a specific firmware: host UI/state, pure Kotlin, resources, docs or CI.
2. It must not replace an engine prebuilt (`.so`, `.jar`, runner binaries) unless the source and a test rig are available.
3. It must be attributable: keep the original commit author and link the upstream issue/PR.
4. Anything that changes guest-visible behavior should be a separate commit and documented in
   [personal-patch.md](personal-patch.md), so it can be dropped when upstream fixes it.

## Older upstreams

See [upstream-of-upstream.md](upstream-of-upstream.md) for the Sunset and uxazu audit: both are older than Plus, their
app fixes are already incorporated, and only the Sunset.37 contrast fix was still missing and was applied.
