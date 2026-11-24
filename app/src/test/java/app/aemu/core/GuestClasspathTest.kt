/* Personal patch, 2026-11-24: mainline APEX class path for stub processes. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GuestClasspathTest {
    private fun root(): File = File.createTempFile("guest-cp", "").apply { delete(); mkdirs() }

    @Test fun bootClasspathOnlyTreesAreUnchanged() {
        val r = root()
        try {
            assertEquals("/system/framework/aemu-stubs.jar",
                GuestClasspath.of(r, "/system/framework/aemu-stubs.jar"))
        } finally { r.deleteRecursively() }
    }

    @Test fun wifiAndConnectivityApexJarsAreAppendedInOrder() {
        val r = root()
        try {
            File(r, GuestClasspath.WIFI_JAR).apply { parentFile?.mkdirs(); writeText("x") }
            File(r, GuestClasspath.CONNECTIVITY_JAR).apply { parentFile?.mkdirs(); writeText("x") }
            assertEquals(
                "/system/framework/aemu-wifi.jar:/${GuestClasspath.WIFI_JAR}:/${GuestClasspath.CONNECTIVITY_JAR}",
                GuestClasspath.of(r, "/system/framework/aemu-wifi.jar"),
            )
        } finally { r.deleteRecursively() }
    }

    @Test fun flattenedSystemApexFallbackIsAccepted() {
        val r = root()
        try {
            File(r, "system/${GuestClasspath.WIFI_JAR}").apply { parentFile?.mkdirs(); writeText("x") }
            assertEquals(
                "/system/framework/aemu-wifi.jar:/system/${GuestClasspath.WIFI_JAR}",
                GuestClasspath.of(r, "/system/framework/aemu-wifi.jar"),
            )
        } finally { r.deleteRecursively() }
    }
}
