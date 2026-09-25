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

package dev.krtirtho.spotube.modules.settings.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Cast
import compose.icons.feathericons.Radio
import compose.icons.feathericons.Repeat
import compose.icons.feathericons.Server
import compose.icons.feathericons.Sliders
import spotube.composeapp.generated.resources.*
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SelectionSettingCard
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.modules.settings.components.SwitchSettingCard
import dev.krtirtho.spotube.modules.settings.components.TextInputSettingCard
import dev.krtirtho.spotube.resources.iconsax.CustomServer
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxEdit
import dev.krtirtho.spotube.resources.iconsax.IconsaxForbidden
import dev.krtirtho.spotube.resources.iconsax.IconsaxMirroringScreen
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeatArrow
import dev.krtirtho.spotube.resources.iconsax.IconsaxVerticalSetting
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.playbackSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    navigatorCommands: NavigationCommands,
    requestLocalNetworkPermission: () -> Unit,
) {
    val streamingFormats = availableAudioFormats(settings.streamingMusicFormat, streamingFormatPresets)
    val streamingQualities = availableAudioQualities(
        format = settings.streamingMusicFormat,
        current = settings.streamingMusicQuality,
    )

    settingsSectionHeader(Res.string.settings_section_playback)
    settingsSectionCard(
        items = listOf(
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_streaming_format_title),
                    subtitle = stringResource(
                        Res.string.settings_streaming_format_subtitle_current,
                        settings.streamingMusicFormat.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxMusicPlay, stringResource(Res.string.settings_streaming_format_title))
                    },
                    selectedOption = settings.streamingMusicFormat,
                    options = streamingFormats,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { format ->
                        settingsViewModel.updateSettings {
                            copy(
                                streamingMusicFormat = format,
                                streamingMusicQuality = format.resolveQuality(streamingMusicQuality),
                            )
                        }
                    }
                )
            },
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_streaming_quality_title),
                    subtitle = stringResource(
                        Res.string.settings_subtitle_current,
                        settings.streamingMusicQuality.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxVerticalSetting, stringResource(Res.string.settings_streaming_quality_title))
                    },
                    selectedOption = settings.streamingMusicQuality,
                    options = streamingQualities,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { quality ->
                        settingsViewModel.updateSettings {
                            copy(streamingMusicQuality = quality)
                        }
                    }
                )
            },
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_enable_endless_playback_title),
                    subtitle = stringResource(Res.string.settings_enable_endless_playback_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxRepeatArrow,
                            stringResource(Res.string.settings_enable_endless_playback_title)
                        )
                    },
                    checked = settings.enableEndlessPlayback,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(enableEndlessPlayback = enabled)
                        }
                    }
                )
            },
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_enable_connect_title),
                    subtitle = stringResource(Res.string.settings_enable_connect_subtitle),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxMirroringScreen, stringResource(Res.string.settings_enable_connect_title))
                    },
                    checked = settings.enableConnect,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(enableConnect = enabled)
                        }
                    }
                )
            },
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_allow_remote_control_title),
                    subtitle = stringResource(Res.string.settings_allow_remote_control_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxMirroringScreen,
                            stringResource(Res.string.settings_allow_remote_control_title)
                        )
                    },
                    checked = settings.allowRemoteControl,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(allowRemoteControl = enabled)
                        }
                        // Request the local network permission when enabling remote control
                        // so that DNS-SD registration can succeed on Android 16+
                        if (enabled) {
                            requestLocalNetworkPermission()
                        }
                    }
                )
            },
            {
                TextInputSettingCard(
                    title = stringResource(Res.string.settings_remote_device_name_title),
                    subtitle = stringResource(
                        Res.string.settings_remote_device_name_subtitle,
                        settings.remoteControlDeviceName.ifBlank { stringResource(Res.string.settings_remote_device_name_default) }
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxEdit, stringResource(Res.string.settings_remote_device_name_title))
                    },
                    value = settings.remoteControlDeviceName,
                    dialogDescription = stringResource(Res.string.settings_remote_device_name_description),
                    placeholder = stringResource(Res.string.settings_remote_device_name_placeholder),
                    normalize = { it.trim() },
                    validate = { _ -> null },
                    onValueSaved = { value ->
                        settingsViewModel.updateSettings {
                            copy(remoteControlDeviceName = value)
                        }
                    }
                )
            },
            {
                val error_whole_number = stringResource(Res.string.settings_error_whole_number)
                val error_port_range = stringResource(Res.string.settings_error_port_range)

                TextInputSettingCard(
                    title = stringResource(Res.string.settings_playback_port_title),
                    subtitle = stringResource(
                        Res.string.settings_playback_port_subtitle_current,
                        settings.playbackProxyServerPort
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.CustomServer, stringResource(Res.string.settings_playback_port_title))
                    },
                    value = settings.playbackProxyServerPort.toString(),
                    dialogDescription = stringResource(Res.string.settings_playback_port_description),
                    placeholder = stringResource(Res.string.settings_playback_port_placeholder),
                    normalize = { it.trim() },
                    validate = { value ->
                        val port = value.toIntOrNull()
                        when {
                            port == null -> error_whole_number
                            port !in 1..65535 -> error_port_range
                            else -> null
                        }
                    },
                    onValueSaved = { value ->
                        settingsViewModel.updateSettings {
                            copy(playbackProxyServerPort = value.toInt())
                        }
                    }
                )
            },
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_blacklist_title),
                    subtitle = stringResource(Res.string.settings_blacklist_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxForbidden,
                            stringResource(Res.string.settings_blacklist_title),
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = FeatherIcons.ChevronRight,
                            contentDescription = stringResource(Res.string.settings_blacklist_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        navigatorCommands.navigateTo(Routes.Blacklist)
                    },
                )
            },
        )
    )
}

