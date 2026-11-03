/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.modules.plugin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowRight
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.plugin.PluginCapability
import org.spotube.spotube.modules.plugin.PluginEntry
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxEye
import org.spotube.spotube.resources.iconsax.IconsaxWifiSquare
import org.spotube.spotube.resources.iconsax.PhosphorDatabaseDuotone
import org.spotube.spotube.resources.iconsax.User
import okio.Path
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumSeparator
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.common_close
import spotube.composeapp.generated.resources.plugin_permissions_api_diff
import spotube.composeapp.generated.resources.plugin_permissions_capability_network_desc
import spotube.composeapp.generated.resources.plugin_permissions_capability_network_title
import spotube.composeapp.generated.resources.plugin_permissions_capability_storage_desc
import spotube.composeapp.generated.resources.plugin_permissions_capability_storage_title
import spotube.composeapp.generated.resources.plugin_permissions_capability_webview_desc
import spotube.composeapp.generated.resources.plugin_permissions_capability_webview_title
import spotube.composeapp.generated.resources.plugin_permissions_compare_title
import spotube.composeapp.generated.resources.plugin_permissions_installed_label
import spotube.composeapp.generated.resources.plugin_permissions_none
import spotube.composeapp.generated.resources.plugin_permissions_requested_title
import spotube.composeapp.generated.resources.plugin_permissions_supplied_label
import spotube.composeapp.generated.resources.plugin_version_label

@Composable
fun PluginPermissionDialog(
    pluginInfo: PluginEntry,
    title: String,
    message: String,
    confirmLabel: String?,
    existingPlugin: PluginEntry? = null,
    logoPath: Path? = null,
    onConfirm: (() -> Unit)? = null,
    isConfirming: Boolean = false,
    onDismiss: () -> Unit) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { PluginDialogTitle(title) },
        dialogTitle = title,
        actions = {
            PluginDialogActions(
                secondaryLabel = stringResource(
                    if (confirmLabel == null) Res.string.common_close else Res.string.common_cancel),
                onSecondary = onDismiss,
                primaryLabel = confirmLabel,
                onPrimary = onConfirm,
                primaryLoading = isConfirming)
        }) {
        PluginDialogBody {
            PluginPanel {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PluginLogoTile(plugin = pluginInfo, logoPath = logoPath)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            pluginInfo.name,
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PluginPill(pluginInfo.author, icon = Iconsax.User)
                            PluginPill(
                                stringResource(Res.string.plugin_version_label, pluginInfo.version),
                                tint = AppTheme.colors.primary)
                        }
                    }
                }
                if (pluginInfo.description.isNotBlank()) {
                    Text(
                        pluginInfo.description,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis)
                }
            }

            PluginCallout(text = message, isWarning = confirmLabel == null)

            existingPlugin?.let {
                InstalledComparisonPanel(installedPlugin = it, incomingPlugin = pluginInfo)
            }

            PluginSectionLabel(
                stringResource(Res.string.plugin_permissions_requested_title),
                badge = pluginInfo.capabilities.size.takeIf { it > 0 }?.toString())

            PluginPanel {
                if (pluginInfo.capabilities.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PluginIconTile(Iconsax.IconsaxCheckCircle)
                        Text(
                            stringResource(Res.string.plugin_permissions_none),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant)
                    }
                } else {
                    pluginInfo.capabilities.forEachIndexed { index, capability ->
                        if (index > 0) MillenniumSeparator()
                        CapabilityRow(capability)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledComparisonPanel(
    installedPlugin: PluginEntry,
    incomingPlugin: PluginEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PluginSectionLabel(stringResource(Res.string.plugin_permissions_compare_title))
        PluginPanel {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    VersionStat(
                        label = stringResource(Res.string.plugin_permissions_installed_label),
                        value = installedPlugin.version)
                    MillenniumIcon(
                        imageVector = FeatherIcons.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AppTheme.colors.onSurfaceVariant)
                    VersionStat(
                        label = stringResource(Res.string.plugin_permissions_supplied_label),
                        value = incomingPlugin.version,
                        highlighted = true)
                }
                Text(
                    stringResource(
                        Res.string.plugin_permissions_api_diff,
                        installedPlugin.apiVersion,
                        incomingPlugin.apiVersion),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VersionStat(label: String, value: String, highlighted: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant)
        Text(
            stringResource(Res.string.plugin_version_label, value),
            style = AppTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (highlighted) AppTheme.colors.primary else AppTheme.colors.onSurface)
    }
}

@Composable
private fun CapabilityRow(capability: PluginCapability) {
    val (icon, label, description) = when (capability) {
        PluginCapability.PERSISTENT_STORAGE -> Triple(
            Iconsax.PhosphorDatabaseDuotone,
            stringResource(Res.string.plugin_permissions_capability_storage_title),
            stringResource(Res.string.plugin_permissions_capability_storage_desc))
        PluginCapability.NETWORK_REQUESTS -> Triple(
            Iconsax.IconsaxWifiSquare,
            stringResource(Res.string.plugin_permissions_capability_network_title),
            stringResource(Res.string.plugin_permissions_capability_network_desc))
        PluginCapability.WEBVIEW -> Triple(
            Iconsax.IconsaxEye,
            stringResource(Res.string.plugin_permissions_capability_webview_title),
            stringResource(Res.string.plugin_permissions_capability_webview_desc))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PluginIconTile(icon)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                label,
                style = AppTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text(
                description,
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant)
        }
    }
}
