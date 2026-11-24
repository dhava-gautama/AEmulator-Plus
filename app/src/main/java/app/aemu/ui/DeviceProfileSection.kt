/* Personal patch, 2026-11-24: device-identity profile picker. Host-side only. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.DeviceProfile
import app.aemu.core.DeviceProfiles
import app.aemu.core.GuestImage
import app.aemu.core.VmSettings

/**
 * A device profile is one coherent identity: product/build/fingerprint plus the screen and (when known) the chip.
 * Selecting one changes what Build.* and the properties report; it does not change the SDK, the ABI or attestation.
 */
@Composable
internal fun DeviceProfileSection(img: GuestImage, s: VmSettings, set: (VmSettings) -> Unit) {
    val selected = DeviceProfiles.current(s, img.api)
    var open by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<DeviceProfile?>(null) }
    var custom by remember { mutableStateOf(false) }
    val warning = selected?.let { DeviceProfiles.warning(it, img) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.vs_device_profile)) },
        supportingContent = {
            Text(selected?.label ?: stringResource(R.string.vs_device_profile_default))
            if (warning != null) Text(warning, color = MaterialTheme.colorScheme.error)
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (selected != null) TextButton(onClick = { details = selected }) { Text(stringResource(R.string.vs_device_profile_details)) }
                TextButton(onClick = { open = true }) { Text(stringResource(R.string.vs_device_profile_change)) }
            }
        },
    )
    Text(
        stringResource(R.string.vs_device_profile_caveat),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.vs_device_profile_title)) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.vs_device_profile_pick), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                ProfileRow(
                    title = stringResource(R.string.vs_device_profile_default),
                    subtitle = stringResource(R.string.vs_device_profile_default_sub),
                    current = s.deviceProfile.isEmpty(),
                ) {
                    set(s.copy(deviceProfile = "", deviceProfileCustom = ""))
                    open = false
                }
                DeviceProfiles.forApi(img.api).forEach { profile ->
                    ProfileRow(
                        title = profile.label,
                        subtitle = stringResource(R.string.vs_device_profile_row, profile.apis.first,
                            profile.apis.last, profile.fingerprint),
                        current = s.deviceProfile == profile.id,
                    ) {
                        set(applyProfile(s, profile))
                        open = false
                    }
                }
                if (DeviceProfiles.forApi(img.api).isEmpty()) {
                    Text(stringResource(R.string.vs_device_profile_none), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { custom = true }) { Text(stringResource(R.string.vs_device_profile_custom)) }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
    )

    if (custom) CustomProfileDialog(
        initial = s.deviceProfileCustom,
        onDismiss = { custom = false },
        onApply = { profile, json ->
            set(applyProfile(s, profile).copy(deviceProfile = DeviceProfile.CUSTOM, deviceProfileCustom = json))
            custom = false
            open = false
        },
    )

    details?.let { p ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text(p.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Field("API", if (p.apis.first == p.apis.last) p.apis.first.toString() else "${p.apis.first}..${p.apis.last}")
                    Field("Fingerprint", p.fingerprint, mono = true)
                    Field("Build ID", p.buildId)
                    Field("Security patch", p.securityPatch)
                    Field("Screen", "${p.width}×${p.height} @ ${p.density} dpi")
                    if (p.soc.isNotBlank()) Field("Chip preset", p.soc)
                    val w = DeviceProfiles.warning(p, img)
                    if (w != null) Text(w, color = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.vs_device_profile_caveat), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { details = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

private fun applyProfile(s: VmSettings, p: DeviceProfile): VmSettings = s.copy(
    deviceProfile = p.id,
    deviceProfileCustom = "",
    width = p.width and -2,
    height = p.height and -2,
    density = p.density,
    hostResolution = false,
    soc = p.soc,
)

@Composable
private fun ProfileRow(title: String, subtitle: String, current: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 2, style = MaterialTheme.typography.bodySmall) },
        trailingContent = if (current) { { Text(stringResource(R.string.vs_device_profile_active), color = MaterialTheme.colorScheme.primary) } } else null,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CustomProfileDialog(initial: String, onDismiss: () -> Unit, onApply: (DeviceProfile, String) -> Unit) {
    val example = """
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
    """.trimIndent()
    var text by remember { mutableStateOf(initial.ifBlank { example }) }
    var error by remember { mutableStateOf<String?>(null) }
    val parsed = remember(text) { DeviceProfile.fromJson(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vs_device_profile_custom_title)) },
        text = {
            Column {
                Text(stringResource(R.string.vs_device_profile_custom_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 260.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    label = { Text("JSON") },
                )
                if (parsed == null && text.isNotBlank()) Text(stringResource(R.string.vs_device_profile_invalid),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (parsed != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("${parsed.label} · API ${parsed.apis.first}..${parsed.apis.last}", style = MaterialTheme.typography.bodySmall)
                    Text(parsed.fingerprint, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            Button(enabled = parsed != null, onClick = {
                val p = parsed
                if (p != null) onApply(p, text)
            }) { Text(stringResource(R.string.vs_device_profile_apply)) }
        },
    )
}

@Composable
private fun Field(label: String, value: String, mono: Boolean = false) {
    Text(
        "$label: $value",
        style = if (mono) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            else MaterialTheme.typography.bodyMedium,
    )
}
