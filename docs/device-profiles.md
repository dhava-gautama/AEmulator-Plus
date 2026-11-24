# Device identity profiles

Added in `personal.3` (2026-11-24). A device profile is a **coherent, host-side identity** for one VM: the product
properties, the build fingerprint and the screen resolution an app reads through `Build.*`. It is not a hardware
emulation and not attestation.

## What it changes

At the next full VM boot, `GuestVm` adds the profile to the property area **after** the firmware's own build props and
after the engine's `oneFingerprint` step. The following keys are set (the same values for the system/vendor/product/odm
partitions where those keys exist):

- `ro.product.brand`, `ro.product.manufacturer`, `ro.product.model`, `ro.product.device`, `ro.product.name`,
  `ro.product.board`;
- `ro.build.id`, `ro.build.display.id`, `ro.build.version.incremental`, `ro.build.version.security_patch`,
  `ro.build.type`, `ro.build.tags`, `ro.build.description`, `ro.build.flavor`, `ro.build.product`;
- `ro.build.fingerprint`, `ro.system.build.fingerprint`, `ro.vendor.build.fingerprint`,
  `ro.product.build.fingerprint`, `ro.odm.build.fingerprint`, `ro.bootimage.build.fingerprint`.

**Never changed:** `ro.build.version.sdk`, `ro.build.version.release`, `ro.product.cpu.abilist*`, `ro.zygote`,
`ro.hardware`, `ro.debuggable`, `ro.secure`, init/`init.svc.*` and every other boot-critical property. A profile cannot
change the Android version, the ABI, the engine or the firmware's hardware support.

Selecting a profile also applies its screen size/density and (when it names one) a `SocProfiles` chip preset. Those are
ordinary VM settings and can be edited afterwards.

## Built-in profiles

| Id | Identity | API | Build |
| --- | --- | --- | --- |
| `pixel6_12` | Pixel 6 | 31–32 | `SP2A.220505.002` (Android 12/12L) |
| `pixel6_13` | Pixel 6 | 33 | `TP1A.220624.021` |
| `pixel7pro_13` | Pixel 7 Pro | 33 | `TQ3A.230805.001.A2` |
| `s23ultra_13` | Galaxy S23 Ultra | 33 | `TP1A.220624.014` / `S918BXXU1AWC8` |
| `s23ultra_14` | Galaxy S23 Ultra | 34 | `UP1A.231005.007` / `S918BXXS3BXAD` |
| `pixel8_14` | Pixel 8 | 34 | `UQ1A.240205.004` |
| `pixel8_15` | Pixel 8 | 35 | `AP3A.241105.007` |
| `pixel9_15` | Pixel 9 | 35 | `AP3A.241105.008` |

The fingerprint is **derived** from the profile fields (`brand/product/device:release/buildId/incremental:type/tags`), so
it cannot disagree with its own build ID, product, type or tags. Profiles are listed only for a guest whose API they
match.

The built-in rows describe real factory builds, but Google's build data changes. A profile that does not boot on your
image can be replaced with a custom one; the app does not download or update profiles.

Pixel rows have no `soc` value: this app has no Tensor chip preset yet, so the guest still reports the firmware/host
chip while the Build identity says Pixel. The Galaxy S23 Ultra rows set `sm8550p` (Snapdragon 8 Gen 2 for Galaxy), which
matches that device's platform and GPU strings.

## Custom profile JSON

Settings › Device profile › **Custom JSON…** accepts:

```json
{
  "id": "custom",
  "label": "My device",
  "api": 33,
  "release": "13",
  "brand": "google",
  "manufacturer": "Google",
  "model": "Pixel 7",
  "device": "panther",
  "product": "panther",
  "board": "panther",
  "buildId": "TQ3A.230805.001",
  "incremental": "10316531",
  "securityPatch": "2023-08-05",
  "width": 1080,
  "height": 2400,
  "density": 416,
  "soc": "",
  "type": "user",
  "tags": "release-keys"
}
```

Required: `api`, `release`, `brand`, `manufacturer`, `model`, `device`, `product`, `board`, `buildId`, `incremental`
and `securityPatch` (YYYY-MM-DD). `api_max` is accepted for a range. `soc` is a preset id from the chip picker or blank.
Strings are sanitised to printable single-line text; the whole JSON is capped at 16 KiB. Only the identity keys above are
produced — a hand-written `ro.hardware` or `ro.build.version.sdk` in the JSON is ignored.

## Caveats

- **Cosmetic only.** Apps that check `Build.MODEL`, `Build.BRAND`, the fingerprint or the reported screen see the
  profile. Apps that check a hardware-backed attestation chain do not; profiles do not pass Play Integrity or
  SafetyNet.
- The guest is still the firmware you imported. Features the firmware lacks (NFC, a fingerprint sensor, a specific
  GPU) are not added.
- A profile's release must match the guest's `ro.build.version.release`; the validator warns when it does not.
- Some vendor daemons read `ro.product.*` for their own logic. Change the profile only on a VM copy if the firmware
  behaves differently.
- The Android SDK and ABI cannot be changed by a profile; pick a profile for the guest's actual API.

## Testing a profile

```bash
adb shell getprop ro.build.fingerprint
adb shell getprop ro.product.model
adb shell getprop ro.build.version.sdk   # unchanged
adb shell getprop ro.build.version.security_patch
adb shell wm size; adb shell wm density
```

Then open Settings › About phone and any app that displays the device model. To go back, choose **Firmware identity
(unchanged)** and reboot the VM.
