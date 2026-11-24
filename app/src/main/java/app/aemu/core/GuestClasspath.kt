/* Personal patch, 2026-11-24: class path for guest stub processes on mainline-modular releases.
 * GPL-3.0; see LICENSE and NOTICE.md.
 *
 * Android 17 GSIs keep some framework classes outside the boot class path: Wi-Fi lives in the com.android.wifi APEX and
 * NetworkAgent in the com.android.tethering APEX. An app_process started with only aemu-wifi.jar/aemu-stubs.jar then
 * fails to resolve WifiConfiguration or NetworkAgent, so the Wi-Fi service never registers and the Ethernet fallback
 * never registers a network either. ApexUnpacker unpacks those packages into <root>/apex/<module>, so their framework
 * jars can be put on the stub's class path. On older releases the files are absent and the boot class path already has
 * the classes; the list is unchanged. */
package app.aemu.core

import java.io.File

object GuestClasspath {
    const val WIFI_JAR = "apex/com.android.wifi/javalib/framework-wifi.jar"
    const val CONNECTIVITY_JAR = "apex/com.android.tethering/javalib/framework-connectivity.jar"

    /** Framework jars that may only exist in APEXes (relative to the guest root). */
    private val MAINLINE = listOf(WIFI_JAR, CONNECTIVITY_JAR)

    /**
     * [base] (a guest path such as /system/framework/aemu-wifi.jar) followed by the framework jars that exist in this
     * tree. A flattened package can also be reached through /system/apex; both places are checked, without duplicates.
     */
    fun of(root: File, vararg bases: String): String {
        val jars = LinkedHashSet<String>()
        for (rel in MAINLINE) for (prefix in listOf("", "system/")) {
            val path = "$prefix$rel"
            if (File(root, path).isFile) jars.add("/$path")
        }
        return (bases.toList() + jars).joinToString(":")
    }
}
