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
import compose.icons.FeatherIcons
import compose.icons.feathericons.Globe
import compose.icons.feathericons.MapPin
import spotube.composeapp.generated.resources.*
import dev.krtirtho.spotube.modules.settings.CountryCode
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.SupportedLanguages
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SelectionSettingCard
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxGlobe
import dev.krtirtho.spotube.resources.iconsax.IconsaxLanguageSquare
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.languageRegionSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_language_region)
    }
    settingsSectionCard(
        items = listOf(
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_language_title),
                    subtitle = stringResource(
                        Res.string.settings_language_subtitle_current,
                        settings.language.displayName
                    ),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxLanguageSquare,
                            stringResource(Res.string.settings_language_title)
                        )
                    },
                    selectedOption = settings.language,
                    options = SupportedLanguages.entries,
                    optionLabel = {
                        stringResource(Res.string.settings_option_name_and_code, it.displayName, it.locale)
                    },
                    onOptionSelected = { value ->
                        settingsViewModel.updateSettings {
                            copy(language = value)
                        }
                    },
                    filter = { item, query -> item.label.contains(query, ignoreCase = true) },
                )
            },
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_country_title),
                    subtitle = stringResource(
                        Res.string.settings_country_subtitle_current,
                        settings.country.displayName
                    ),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxGlobe,
                            stringResource(Res.string.settings_country_title)
                        )
                    },
                    selectedOption = settings.country,
                    options = CountryCode.entries,
                    optionLabel = {
                        stringResource(Res.string.settings_option_name_and_code, it.displayName, it.code)
                    },
                    onOptionSelected = { value ->
                        settingsViewModel.updateSettings {
                            copy(country = value)
                        }
                    },
                    filter = { item, query -> item.label.contains(query, ignoreCase = true) },
                )
            },
        )
    )
}

