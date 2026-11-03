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

import androidx.compose.foundation.lazy.LazyListScope
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.modules.support.donationOptions
import org.spotube.spotube.modules.support.openDonation
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_section_support
import spotube.composeapp.generated.resources.settings_support_reminders_subtitle
import spotube.composeapp.generated.resources.settings_support_reminders_title

/** Support settings: the periodic reminder toggle plus the donation destinations. */
internal fun LazyListScope.supportSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_support)
    }
    settingsSectionCard(
        items = buildList {
            add {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_support_reminders_title),
                    subtitle = stringResource(Res.string.settings_support_reminders_subtitle),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxHeart2,
                            contentDescription = stringResource(
                                Res.string.settings_support_reminders_title))
                    },
                    checked = settings.supportRemindersEnabled,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(supportRemindersEnabled = enabled)
                        }
                    })
            }
            donationOptions.forEach { option ->
                add {
                    SettingCardItem(
                        title = stringResource(option.title),
                        subtitle = stringResource(option.subtitle),
                        icon = {
                            SettingsItemIcon(
                                imageVector = option.icon,
                                contentDescription = stringResource(option.title))
                        },
                        onClick = { openDonation(option) })
                }
            }
        })
}
