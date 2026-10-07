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
import spotube.composeapp.generated.resources.*
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SwitchSettingCard
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxImportArrow2
import dev.krtirtho.spotube.resources.iconsax.LineDiscord
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.desktopSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_desktop)
    }
    settingsSectionCard(
        items = listOf(
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_desktop_minimize_title),
                    subtitle = stringResource(Res.string.settings_desktop_minimize_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxImportArrow2,
                            stringResource(Res.string.settings_desktop_minimize_title)
                        )
                    },
                    checked = settings.minimizeToTray,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(minimizeToTray = enabled)
                        }
                    }
                )
            },
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_desktop_discord_title),
                    subtitle = stringResource(Res.string.settings_desktop_discord_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.LineDiscord,
                            stringResource(Res.string.settings_desktop_discord_title)
                        )
                    },
                    checked = settings.discordRichPresence,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(discordRichPresence = enabled)
                        }
                    }
                )
            },
        )
    )
}

