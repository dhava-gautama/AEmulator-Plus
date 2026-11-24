/* Personal patch, 2026-11-24: device profiles. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfilesTest {
    private fun image(api: Int = 33, release: String = "13") = GuestImage(
        id = "test", name = "Test", release = release, api = api, brand = "firmware", model = "Firmware",
        skin = "AOSP", engine = Engine.N64, abi = "arm64-v8a", arch = GuestArch.ARM, bootclasspath = "",
        exports = emptyMap(), dirs = emptyList(), services = emptyList(), sdcardPath = "/sdcard",
        settings = VmSettings(), createdAt = 0,
    )

    @Test fun firmwareIdentityIsTheDefault() {
        assertNull(DeviceProfiles.current(VmSettings(), 33))
        assertNull(DeviceProfiles.byId(""))
        assertNull(DeviceProfiles.byId("missing"))
    }

    @Test fun builtInProfilesAreMatchedToTheGuestApi() {
        assertEquals(listOf("pixel6_12"), DeviceProfiles.forApi(31).map { it.id })
        assertEquals(listOf("pixel6_12"), DeviceProfiles.forApi(32).map { it.id })
        assertTrue(DeviceProfiles.forApi(33).map { it.id }.containsAll(listOf("pixel6_13", "pixel7pro_13")))
        assertEquals(listOf("pixel8_14"), DeviceProfiles.forApi(34).map { it.id })
        assertEquals(listOf("pixel8_15", "pixel9_15"), DeviceProfiles.forApi(35).map { it.id })
        assertNull(DeviceProfiles.current(VmSettings(deviceProfile = "pixel8_14"), 33))
    }

    @Test fun profilePropertiesAreCoherentAndDoNotTouchBootCriticalKeys() {
        val p = DeviceProfiles.byId("pixel7pro_13")!!
        val props = p.props()
        assertEquals("google/cheetah/cheetah:13/TQ3A.230805.001.A2/10385117:user/release-keys", props["ro.build.fingerprint"])
        assertEquals(props["ro.build.fingerprint"], props["ro.system.build.fingerprint"])
        assertEquals(props["ro.build.fingerprint"], props["ro.vendor.build.fingerprint"])
        assertEquals("Pixel 7 Pro", props["ro.product.model"])
        assertEquals("13", p.release)
        for (key in listOf("ro.build.version.sdk", "ro.build.version.release", "ro.product.cpu.abilist",
            "ro.product.cpu.abilist64", "ro.zygote", "ro.hardware", "ro.debuggable", "ro.secure",
            "persist.sys.usb.config", "init.svc.zygote")) {
            assertFalse("profile must not set $key", props.containsKey(key))
        }
    }

    @Test fun customProfileRoundTrips() {
        val json = DeviceProfiles.byId("pixel8_14")!!.toJson().put("id", "custom").toString()
        val p = DeviceProfile.fromJson(json)
        assertNotNull(p)
        assertEquals("Pixel 8", p!!.model)
        assertEquals(34, p.apis.first)
        assertNotNull(DeviceProfiles.current(VmSettings(deviceProfile = "custom", deviceProfileCustom = json), 34))
        assertNull(DeviceProfiles.current(VmSettings(deviceProfile = "custom", deviceProfileCustom = json), 33))
    }

    @Test fun malformedOrIncompleteCustomProfilesAreRejected() {
        assertNull(DeviceProfile.fromJson(""))
        assertNull(DeviceProfile.fromJson("{}"))
        assertNull(DeviceProfile.fromJson("""{"api":33,"brand":"x"}"""))
        assertNull(DeviceProfile.fromJson("""{"api":99,"brand":"x","manufacturer":"x","model":"x","device":"x","product":"x","board":"x","buildId":"x","incremental":"1","release":"13","securityPatch":"2024-01-01"}"""))
        assertNull(DeviceProfile.fromJson("""{"api":33,"brand":"x","manufacturer":"x","model":"x","device":"x","product":"x","board":"x","buildId":"x","incremental":"1","release":"13","securityPatch":"not-a-date"}"""))
    }

    @Test fun settingsJsonKeepsTheSelection() {
        val s = VmSettings(deviceProfile = "pixel8_15", deviceProfileCustom = "")
        val back = VmSettings.fromJson(s.toJson())
        assertEquals("pixel8_15", back.deviceProfile)
        assertNull(DeviceProfiles.current(back, 34))
        assertNotNull(DeviceProfiles.current(back, 35))
        val unknown = VmSettings.fromJson(JSONObject().put("deviceProfile", "not-built-in"))
        assertEquals("", unknown.deviceProfile)
    }

    @Test fun warningReportsApiAndReleaseMismatches() {
        val p = DeviceProfiles.byId("pixel6_13")!!
        assertNull(DeviceProfiles.warning(p, image(33, "13")))
        assertNotNull(DeviceProfiles.warning(p, image(32, "12")))
        assertNotNull(DeviceProfiles.warning(p, image(33, "12")))
    }
}
