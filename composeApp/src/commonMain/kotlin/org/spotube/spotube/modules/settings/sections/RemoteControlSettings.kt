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

package org.spotube.spotube.modules.settings.sections

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.modules.settings.components.TextInputSettingCard
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_allow_remote_control_subtitle
import spotube.composeapp.generated.resources.settings_allow_remote_control_title
import spotube.composeapp.generated.resources.settings_remote_device_name_default
import spotube.composeapp.generated.resources.settings_remote_device_name_description
import spotube.composeapp.generated.resources.settings_remote_device_name_placeholder
import spotube.composeapp.generated.resources.settings_remote_device_name_subtitle
import spotube.composeapp.generated.resources.settings_remote_device_name_title

/**
 * "Allow remote control" and "Device name" cards. Shared by the Playback settings section and the
 * Devices screen so both stay identical.
 */
internal fun remoteControlSettingCards(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    requestLocalNetworkPermission: () -> Unit
): List<@Composable () -> Unit> = listOf(
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
    }
)
