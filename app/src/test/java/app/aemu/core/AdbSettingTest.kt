/* Personal patch, 2026-11-23: network ADB is a per-VM setting (upstream issue #126). GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdbSettingTest {
    @Test fun newAndOldMachinesKeepNetworkAdbOffByDefault() {
        assertFalse(VmSettings().adb)
        assertFalse(VmSettings.fromJson(JSONObject()).adb)
        // image.json written by 0.0.0.3.16 and older has no "adb" key
        assertFalse(VmSettings.fromJson(JSONObject().put("gpu", true)).adb)
    }

    @Test fun enabledSwitchSurvivesTheImageJsonRoundTrip() {
        val saved = VmSettings(adb = true)
        assertTrue(VmSettings.fromJson(saved.toJson()).adb)
    }

    @Test fun disabledSwitchIsWrittenExplicitlySoTurningItOffSticks() {
        val json = VmSettings(adb = false).toJson()
        assertTrue(json.has("adb"))
        assertFalse(VmSettings.fromJson(json).adb)
    }
}
