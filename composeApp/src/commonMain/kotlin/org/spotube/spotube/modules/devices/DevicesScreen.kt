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

package org.spotube.spotube.modules.devices


import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSpokeSpinner
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.core.discovery.rememberLocalNetworkPermissionRequester
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.modules.settings.sections.SettingsItemIcon
import org.spotube.spotube.modules.settings.sections.remoteControlSettingCards
import org.spotube.spotube.modules.settings.sections.settingsSectionCard
import org.spotube.spotube.modules.settings.sections.settingsSectionHeader
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxWifiSquare
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.devices_disconnect
import spotube.composeapp.generated.resources.devices_empty
import spotube.composeapp.generated.resources.devices_empty_hint
import spotube.composeapp.generated.resources.devices_open_remote
import spotube.composeapp.generated.resources.devices_refresh
import spotube.composeapp.generated.resources.devices_resolving
import spotube.composeapp.generated.resources.devices_search_again
import spotube.composeapp.generated.resources.devices_searching
import spotube.composeapp.generated.resources.devices_searching_more
import spotube.composeapp.generated.resources.devices_section_available
import spotube.composeapp.generated.resources.devices_section_connected
import spotube.composeapp.generated.resources.devices_section_this_device
import spotube.composeapp.generated.resources.devices_status_connected
import spotube.composeapp.generated.resources.devices_title
import spotube.composeapp.generated.resources.devices_unknown_device
import spotube.composeapp.generated.resources.jam_error_dismiss

private val ContentMaxWidth = 720.dp

@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val shellBottomInset = LocalAppShellBottomInset.current

    DisposableEffect(Unit) {
        // Android 16+ needs NEARBY_WIFI_DEVICES granted at runtime before mDNS works.
        requestLocalNetworkPermission()
        viewModel.startDiscovery()
        onDispose {
            // The remote connection itself outlives this screen; only discovery stops.
            viewModel.stopDiscovery()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MillenniumTheme.colors.background)
    ) {
        ApplicationMainBar(
            backButton = true,
            title = { Text(stringResource(Res.string.devices_title)) },
            actions = {
                MillenniumIconButton(
                    onClick = viewModel::startDiscovery,
                    enabled = !state.isConnecting,
                    loading = state.isDiscovering,
                    style = MillenniumIconButtonStyle.Quiet
                ) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxRefreshRight,
                        contentDescription = stringResource(Res.string.devices_refresh)
                    )
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = shellBottomInset + 16.dp
                )
            ) {
                state.error?.let { error ->
                    item {
                        ErrorBanner(
                            message = error.rawMessage
                                ?: stringResource(error.message, *error.args.toTypedArray()),
                            onDismiss = viewModel::clearError
                        )
                    }
                }

                state.connectedDevice?.let { device ->
                    settingsSectionHeader(Res.string.devices_section_connected)
                    item {
                        ConnectedDeviceCard(
                            device = device,
                            onOpenRemote = viewModel::openRemote,
                            onDisconnect = viewModel::disconnect
                        )
                    }
                }

                if (state.devices.isNotEmpty()) {
                    settingsSectionHeader(Res.string.devices_section_available)
                    settingsSectionCard(
                        items = state.devices.map { device ->
                            {
                                DeviceTile(
                                    device = device,
                                    enabled = !state.isConnecting || device.isConnecting,
                                    onClick = { viewModel.onDeviceClick(device.key) }
                                )
                            }
                        }
                    )
                    if (state.isDiscovering) {
                        item {
                            Text(
                                text = stringResource(Res.string.devices_searching_more),
                                style = AppTheme.typography.bodySmall,
                                color = MillenniumTheme.colors.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                } else if (state.showEmptyState) {
                    item {
                        EmptyState(
                            isDiscovering = state.isDiscovering,
                            onSearchAgain = viewModel::startDiscovery
                        )
                    }
                }

                // Same cards as Settings > Playback, so making this device controllable (or renaming
                // it) doesn't require a detour through Settings.
                settings?.let { current ->
                    settingsSectionHeader(Res.string.devices_section_this_device)
                    settingsSectionCard(
                        items = remoteControlSettingCards(current, settingsViewModel, requestLocalNetworkPermission)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceTile(
    device: DeviceItemUi,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val name = device.name ?: stringResource(Res.string.devices_unknown_device)
    SettingCardItem(
        enabled = enabled,
        title = name,
        subtitle = device.address ?: stringResource(Res.string.devices_resolving),
        icon = { SettingsItemIcon(Iconsax.IconsaxMirroringScreen, name) },
        trailingContent = {
            when {
                device.isConnecting -> MillenniumSpokeSpinner(size = 20.dp)
                device.isConnected -> Text(
                    text = stringResource(Res.string.devices_status_connected),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.primary
                )
                else -> MillenniumIcon(
                    imageVector = FeatherIcons.ChevronRight,
                    contentDescription = null,
                    tint = AppTheme.colors.onSurfaceVariant
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun LazyItemScope.ConnectedDeviceCard(
    device: ConnectedDeviceUi,
    onOpenRemote: () -> Unit,
    onDisconnect: () -> Unit
) {
    val name = device.name ?: stringResource(Res.string.devices_unknown_device)
    MillenniumCard(
        modifier = Modifier
            .fillParentMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        maxWidth = ContentMaxWidth,
        contentPadding = PaddingValues(16.dp),
        contentSpacing = 16.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsItemIcon(Iconsax.IconsaxMirroringScreen, name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = AppTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.address,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            MillenniumButton(
                onClick = onDisconnect,
                loading = device.isDisconnecting,
                style = MillenniumButtonStyle.Secondary
            ) {
                Text(stringResource(Res.string.devices_disconnect))
            }
            MillenniumButton(
                onClick = onOpenRemote,
                style = MillenniumButtonStyle.Primary
            ) {
                Text(stringResource(Res.string.devices_open_remote))
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(AppTheme.shapes.medium)
            .background(AppTheme.colors.errorContainer)
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MillenniumIcon(
            imageVector = Iconsax.IconsaxInformation,
            contentDescription = null,
            tint = AppTheme.colors.onErrorContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        MillenniumIconButton(
            onClick = onDismiss,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxCloseSquare,
                contentDescription = stringResource(Res.string.jam_error_dismiss),
                tint = AppTheme.colors.onErrorContainer
            )
        }
    }
}

@Composable
private fun LazyItemScope.EmptyState(
    isDiscovering: Boolean,
    onSearchAgain: () -> Unit
) {
    // Fixed padding rather than filling the viewport, so the "This device" section below stays visible.
    Box(
        modifier = Modifier
            .fillParentMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isDiscovering) {
                    MillenniumSpokeSpinner(size = 32.dp)
                } else {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxWifiSquare,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = stringResource(
                    if (isDiscovering) Res.string.devices_searching else Res.string.devices_empty
                ),
                style = AppTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(Res.string.devices_empty_hint),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!isDiscovering) {
                MillenniumButton(
                    onClick = onSearchAgain,
                    style = MillenniumButtonStyle.Secondary,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(Res.string.devices_search_again))
                }
            }
        }
    }
}
