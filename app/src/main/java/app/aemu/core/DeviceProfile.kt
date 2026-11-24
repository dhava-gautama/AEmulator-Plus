/* Personal patch, 2026-11-24: coherent device-identity profiles for the guest.
 * GPL-3.0; see LICENSE and NOTICE.md.
 *
 * A profile only changes what the guest reports through Build.* and the properties apps read. It does not
 * emulate hardware features, does not change ro.build.version.sdk / the ABI, and does not create an attestation
 * chain (see docs/device-profiles.md). */
package app.aemu.core

import org.json.JSONObject

/**
 * One coherent device identity. The build fingerprint is derived from the fields, so a profile can never describe
 * a fingerprint that disagrees with its own build id/product/device/type/tags.
 */
data class DeviceProfile(
    val id: String,
    val label: String,
    val apis: IntRange,
    /** ro.build.version.release of the identity, e.g. "13"; the guest's own SDK/release is never changed. */
    val release: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val buildId: String,
    val incremental: String,
    val securityPatch: String,
    val width: Int,
    val height: Int,
    val density: Int,
    /** id of a SocProfiles preset, or blank for the firmware/host default. */
    val soc: String = "",
    val type: String = "user",
    val tags: String = "release-keys",
) {
    val fingerprint: String
        get() = "$brand/$product/$device:$release/$buildId/$incremental:$type/$tags"
    val description: String
        get() = "$product-$type $release $buildId $incremental $tags"
    val flavor: String
        get() = "$product-$type"

    /**
     * Properties handed to the guest at boot. Identity only: no sdk, abi, zygote, hardware, debug or init property is
     * part of this map, so a profile cannot change how the engine or the firmware starts.
     */
    fun props(): Map<String, String> {
        val fp = fingerprint
        val out = LinkedHashMap<String, String>()
        fun identity(prefix: String) {
            out["$prefix.product.brand"] = brand
            out["$prefix.product.manufacturer"] = manufacturer
            out["$prefix.product.model"] = model
            out["$prefix.product.device"] = device
            out["$prefix.product.name"] = product
            out["$prefix.product.board"] = board
            out["$prefix.build.id"] = buildId
            out["$prefix.build.version.incremental"] = incremental
            out["$prefix.build.version.security_patch"] = securityPatch
            out["$prefix.build.fingerprint"] = fp
        }
        identity("ro")
        identity("ro.system")
        identity("ro.vendor")
        identity("ro.product")
        identity("ro.odm")
        out["ro.bootimage.build.fingerprint"] = fp
        out["ro.bootimage.build.id"] = buildId
        out["ro.bootimage.build.version.incremental"] = incremental
        out["ro.bootimage.build.version.security_patch"] = securityPatch
        out["ro.build.display.id"] = buildId
        out["ro.build.type"] = type
        out["ro.build.tags"] = tags
        out["ro.build.description"] = description
        out["ro.build.flavor"] = flavor
        out["ro.build.product"] = device
        return out
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("label", label).put("api", apis.first).put("api_max", apis.last)
        .put("release", release).put("brand", brand).put("manufacturer", manufacturer).put("model", model)
        .put("device", device).put("product", product).put("board", board)
        .put("buildId", buildId).put("incremental", incremental).put("securityPatch", securityPatch)
        .put("width", width).put("height", height).put("density", density).put("soc", soc)
        .put("type", type).put("tags", tags)

    companion object {
        const val MAX_JSON = 16 * 1024
        const val CUSTOM = "custom"

        private fun line(value: String, max: Int): String =
            value.filter { it in ' '..'~' }.trim().take(max)

        private fun token(value: String, max: Int): String =
            value.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '.' || it == '_' || it == '-' }.take(max)

        /** Parse and sanitise a custom profile. Null for malformed or incomplete input. */
        fun fromJson(text: String): DeviceProfile? = runCatching {
            if (text.length > MAX_JSON) return@runCatching null
            val o = JSONObject(text)
            val api = o.optInt("api", 0)
            val apiMax = o.optInt("api_max", api)
            if (api !in 21..37 || apiMax !in api..37) return@runCatching null
            val id = token(o.optString("id", CUSTOM), 32).ifBlank { CUSTOM }
            val label = line(o.optString("label", ""), 48).ifBlank { "Custom device" }
            val release = token(o.optString("release", ""), 8)
            val brand = token(o.optString("brand", ""), 24)
            val manufacturer = line(o.optString("manufacturer", ""), 32)
            val model = line(o.optString("model", ""), 48)
            val device = token(o.optString("device", ""), 32)
            val product = token(o.optString("product", ""), 32)
            val board = token(o.optString("board", ""), 32)
            val buildId = token(o.optString("buildId", ""), 48)
            val incremental = token(o.optString("incremental", ""), 48)
            val patch = o.optString("securityPatch", "").trim()
            val type = token(o.optString("type", "user"), 16).ifBlank { "user" }
            val tags = token(o.optString("tags", "release-keys"), 24).ifBlank { "release-keys" }
            if (release.isEmpty() || brand.isEmpty() || manufacturer.isEmpty() || model.isEmpty() ||
                device.isEmpty() || product.isEmpty() || board.isEmpty() || buildId.isEmpty() || incremental.isEmpty()) return@runCatching null
            if (!patch.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return@runCatching null
            val width = o.optInt("width", 1080).coerceIn(320, 4320) and -2
            val height = o.optInt("height", 2400).coerceIn(320, 4320) and -2
            val density = o.optInt("density", 420).coerceIn(120, 640)
            val soc = token(o.optString("soc", ""), 32).takeIf { it.isEmpty() || SocProfiles.preset(it) != null }.orEmpty()
            DeviceProfile(id, label, api..apiMax, release, brand, manufacturer, model, device, product, board,
                buildId, incremental, patch, width, height, density, soc, type, tags)
        }.getOrNull()
    }
}

/** The built-in identities and the selection logic. Host-side only; no native or firmware change. */
object DeviceProfiles {
    /**
     * One entry per exact identity. `apis` may cover a range when the same identity was shipped for more than one
     * SDK (Pixel 6's 12/12L build). The build fingerprint still names the build it came from.
     */
    val BUILT_IN: List<DeviceProfile> = listOf(
        DeviceProfile(
            id = "pixel6_12", label = "Pixel 6 (Android 12/12L)", apis = 31..32, release = "12",
            brand = "google", manufacturer = "Google", model = "Pixel 6", device = "oriole", product = "oriole",
            board = "oriole", buildId = "SP2A.220505.002", incremental = "8353555", securityPatch = "2022-05-05",
            width = 1080, height = 2400, density = 411,
        ),
        DeviceProfile(
            id = "pixel6_13", label = "Pixel 6 (Android 13)", apis = 33..33, release = "13",
            brand = "google", manufacturer = "Google", model = "Pixel 6", device = "oriole", product = "oriole",
            board = "oriole", buildId = "TP1A.220624.021", incremental = "8877034", securityPatch = "2022-08-05",
            width = 1080, height = 2400, density = 411,
        ),
        DeviceProfile(
            id = "pixel7pro_13", label = "Pixel 7 Pro (Android 13)", apis = 33..33, release = "13",
            brand = "google", manufacturer = "Google", model = "Pixel 7 Pro", device = "cheetah", product = "cheetah",
            board = "cheetah", buildId = "TQ3A.230805.001.A2", incremental = "10385117", securityPatch = "2023-08-05",
            width = 1440, height = 3120, density = 512,
        ),
        DeviceProfile(
            id = "s23ultra_13", label = "Galaxy S23 Ultra (Android 13)", apis = 33..33, release = "13",
            brand = "samsung", manufacturer = "samsung", model = "SM-S918B", device = "qssi", product = "dm3qxxx",
            board = "kalama", buildId = "TP1A.220624.014", incremental = "S918BXXU1AWC8", securityPatch = "2023-04-01",
            width = 1440, height = 3088, density = 450, soc = "sm8550p",
        ),
        DeviceProfile(
            id = "s23ultra_14", label = "Galaxy S23 Ultra (Android 14)", apis = 34..34, release = "14",
            brand = "samsung", manufacturer = "samsung", model = "SM-S918B", device = "qssi", product = "dm3qxxx",
            board = "kalama", buildId = "UP1A.231005.007", incremental = "S918BXXS3BXAD", securityPatch = "2024-02-01",
            width = 1440, height = 3088, density = 450, soc = "sm8550p",
        ),
        DeviceProfile(
            id = "pixel8_14", label = "Pixel 8 (Android 14)", apis = 34..34, release = "14",
            brand = "google", manufacturer = "Google", model = "Pixel 8", device = "shiba", product = "shiba",
            board = "shiba", buildId = "UQ1A.240205.004", incremental = "11269751", securityPatch = "2024-02-05",
            width = 1080, height = 2400, density = 428,
        ),
        DeviceProfile(
            id = "pixel8_15", label = "Pixel 8 (Android 15)", apis = 35..35, release = "15",
            brand = "google", manufacturer = "Google", model = "Pixel 8", device = "shiba", product = "shiba",
            board = "shiba", buildId = "AP3A.241105.007", incremental = "12470370", securityPatch = "2024-11-05",
            width = 1080, height = 2400, density = 428,
        ),
        DeviceProfile(
            id = "pixel9_15", label = "Pixel 9 (Android 15)", apis = 35..35, release = "15",
            brand = "google", manufacturer = "Google", model = "Pixel 9", device = "tokay", product = "tokay",
            board = "tokay", buildId = "AP3A.241105.008", incremental = "12485168", securityPatch = "2024-11-05",
            width = 1080, height = 2424, density = 422,
        ),
    )

    fun byId(id: String): DeviceProfile? = BUILT_IN.firstOrNull { it.id == id }

    /** Built-in profiles whose identity matches [api]. */
    fun forApi(api: Int): List<DeviceProfile> = BUILT_IN.filter { api in it.apis }

    /** The profile selected in [settings], parsed for the guest's [api], or null for the firmware identity. */
    fun current(settings: VmSettings, api: Int): DeviceProfile? {
        val selected = settings.deviceProfile
        if (selected.isEmpty()) return null
        val profile = if (selected == DeviceProfile.CUSTOM) DeviceProfile.fromJson(settings.deviceProfileCustom)
            else byId(selected)
        return profile?.takeIf { api in it.apis }
    }

    /** A one-line reason the profile cannot be used as-is, or null when the basic identity is coherent. */
    fun warning(profile: DeviceProfile, img: GuestImage): String? = when {
        img.api !in profile.apis -> {
            val range = if (profile.apis.last != profile.apis.first) "${profile.apis.first}..${profile.apis.last}"
                else profile.apis.first.toString()
            "This profile is for API $range, but the guest is API ${img.api}."
        }
        profile.soc.isNotBlank() && SocProfiles.preset(profile.soc) == null -> "The chip preset '${profile.soc}' is not in this build."
        !img.release.trim().startsWith(profile.release) -> "The guest reports Android ${img.release}, the profile is Android ${profile.release}."
        else -> null
    }
}
