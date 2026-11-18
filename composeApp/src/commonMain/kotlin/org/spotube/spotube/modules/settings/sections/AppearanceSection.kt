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

import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.isLightAppearance
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SelectionSettingCard
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxColorSwatch
import org.spotube.spotube.resources.iconsax.IconsaxMagic
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumText
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumThemeConfig
import org.spotube.millennium.theme.resolve
import spotube.composeapp.generated.resources.*

internal fun LazyListScope.appearanceSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true,
    /** When set (compact screens), Appearance is its own page opened from a single row instead of an inline section. */
    themesPageNavigation: NavigationCommands? = null) {
    if (themesPageNavigation != null) {
        settingsSectionCard(
            items = listOf {
                SettingCardItem(
                    title = stringResource(Res.string.settings_section_appearance),
                    subtitle = stringResource(
                        Res.string.settings_theme_subtitle_current,
                        settings.theme.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxColorSwatch, stringResource(Res.string.settings_section_appearance))
                    },
                    trailingContent = {
                        AppIcon(
                            imageVector = FeatherIcons.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.onSurfaceVariant)
                    },
                    onClick = { themesPageNavigation.navigateTo(Routes.ThemeSettings) })
            })
        return
    }
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_appearance)
    }
    settingsSectionCard(items = listOf { ThemeModeCard(settings, settingsViewModel) })
    item { ThemesPanel() }
}

/** The Light / Dark / System choice. Inline on large screens, at the top of the Appearance page on compact ones. */
@Composable
internal fun ThemeModeCard(settings: UserSettings, settingsViewModel: SettingsViewModel) {
    SelectionSettingCard(
        title = stringResource(Res.string.settings_theme_title),
        subtitle = stringResource(
            Res.string.settings_theme_subtitle_current,
            settings.theme.displayLabel()
        ),
        icon = {
            SettingsItemIcon(Iconsax.IconsaxColorSwatch, stringResource(Res.string.settings_theme_title))
        },
        selectedOption = settings.theme,
        options = Theme.entries,
        optionLabel = { it.displayLabel() },
        onOptionSelected = { theme ->
            settingsViewModel.updateSettings {
                copy(theme = theme)
            }
        }
    )
}

@Composable
private fun Theme.displayLabel(): String {
    return when (this) {
        Theme.LIGHT -> stringResource(Res.string.settings_theme_light)
        Theme.DARK -> stringResource(Res.string.settings_theme_dark)
        Theme.SYSTEM -> stringResource(Res.string.settings_theme_system)
    }
}
