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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.isLightAppearance
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UiStyle
import org.spotube.spotube.core.ui.theming.LocalModernFonts
import org.spotube.spotube.core.ui.theming.modernColorSchemes
import org.spotube.spotube.modules.settings.ModernAccent
import org.spotube.spotube.core.ui.theming.modernThemeComponents
import org.spotube.spotube.resources.iconsax.IconsaxColorsSquare
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
    includeHeader: Boolean = true) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_appearance)
    }
    settingsSectionCard(
        items = listOf(
            {
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
            },
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_ui_style_title),
                    subtitle = stringResource(
                        Res.string.settings_ui_style_subtitle_current,
                        settings.uiStyle.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxColorsSquare, stringResource(Res.string.settings_ui_style_title))
                    },
                    selectedOption = settings.uiStyle,
                    options = UiStyle.entries,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { style ->
                        settingsViewModel.updateSettings {
                            copy(uiStyle = style)
                        }
                    }
                )
            },
            {
                AppearanceSettingCard(
                    selectedPalette = settings.palette,
                    selectedAccent = settings.modernAccent,
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxMagic,
                            stringResource(Res.string.settings_appearance_title)
                        )
                    },
                    onSaved = { palette, accent, modern ->
                        settingsViewModel.updateSettings {
                            // Classic has no extra accents, so it leaves the Modern one as it is.
                            if (modern) copy(palette = palette, modernAccent = accent)
                            else copy(palette = palette)
                        }
                    })
            })
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

@Composable
private fun UiStyle.displayLabel(): String = when (this) {
    UiStyle.CLASSIC -> stringResource(Res.string.settings_ui_style_classic)
    UiStyle.MODERN -> stringResource(Res.string.settings_ui_style_modern)
}

/** The role sets of the active interface style, so swatches show the accent as it will look. */
@Composable
private fun activeColorSchemes(accent: ModernAccent? = null): MillenniumColorSchemes =
    if (AppTheme.isModern) modernColorSchemes(accent) else MillenniumColorSchemes()

@Composable
private fun ModernAccent.displayLabel(): String = when (this) {
    ModernAccent.LAVENDER -> stringResource(Res.string.settings_accent_lavender)
    ModernAccent.ROSE -> stringResource(Res.string.settings_accent_rose)
    ModernAccent.RED -> stringResource(Res.string.settings_accent_red)
    ModernAccent.AMBER -> stringResource(Res.string.settings_accent_amber)
    ModernAccent.MINT -> stringResource(Res.string.settings_accent_mint)
    ModernAccent.TEAL -> stringResource(Res.string.settings_accent_teal)
    ModernAccent.SKY -> stringResource(Res.string.settings_accent_sky)
    ModernAccent.ORCHID -> stringResource(Res.string.settings_accent_orchid)
}

@Composable
private fun MillenniumPalette.displayLabel(): String = when (this) {
    MillenniumPalette.Blue -> stringResource(Res.string.settings_palette_blue)
    MillenniumPalette.Orange -> stringResource(Res.string.settings_palette_orange)
    MillenniumPalette.Lime -> stringResource(Res.string.settings_palette_lime)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettingCard(
    selectedPalette: MillenniumPalette,
    selectedAccent: ModernAccent?,
    icon: (@Composable () -> Unit)? = null,
    onSaved: (palette: MillenniumPalette, accent: ModernAccent?, modern: Boolean) -> Unit) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val modern = AppTheme.isModern
    // Extra accents only exist in the Modern style.
    val shownAccent = selectedAccent.takeIf { modern }

    SettingCardItem(
        title = stringResource(Res.string.settings_appearance_title),
        subtitle = stringResource(
            Res.string.settings_appearance_subtitle_palette,
            shownAccent?.displayLabel() ?: selectedPalette.displayLabel()),
        icon = icon,
        trailingContent = {
            PaletteTrailingSwatch(selectedPalette, shownAccent)
        },
        onClick = { isDialogOpen = true })

    if (isDialogOpen) {
        var draftPalette by remember(selectedPalette, isDialogOpen) {
            mutableStateOf(selectedPalette)
        }
        var draftAccent by remember(shownAccent, isDialogOpen) {
            mutableStateOf(shownAccent)
        }

        MillenniumDialog(visible = true, onDismissRequest = { isDialogOpen = false }) {
            MillenniumDialogHeader {
                MillenniumDialogTitle(stringResource(Res.string.settings_palette_dialog_title))
                MillenniumDialogClose()
            }
            MillenniumDialogBody {
            MillenniumText(
                stringResource(Res.string.settings_palette_dialog_description),
                color = AppTheme.colors.onSurfaceVariant)
            MillenniumText(
                stringResource(Res.string.settings_palette_label),
                color = AppTheme.colors.onSurface)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MillenniumPalette.entries.forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        accent = null,
                        label = palette.displayLabel(),
                        selected = palette == draftPalette && draftAccent == null,
                        onClick = {
                            draftPalette = palette
                            draftAccent = null
                        })
                }
            }
            if (modern) {
                MillenniumText(
                    stringResource(Res.string.settings_accent_label),
                    color = AppTheme.colors.onSurface)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernAccent.entries.forEach { accent ->
                        PaletteSwatch(
                            palette = draftPalette,
                            accent = accent,
                            label = accent.displayLabel(),
                            selected = accent == draftAccent,
                            onClick = { draftAccent = accent })
                    }
                }
            }

            PalettePreview(draftPalette, draftAccent)
            }
            MillenniumDialogFooter {
                MillenniumButton(
                    onClick = { isDialogOpen = false },
                    style = MillenniumButtonStyle.Secondary) {
                    MillenniumText(stringResource(Res.string.common_cancel))
                }
                MillenniumButton(
                    onClick = {
                        onSaved(draftPalette, draftAccent, modern)
                        isDialogOpen = false
                    }) {
                    MillenniumText(stringResource(Res.string.common_save))
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: MillenniumPalette,
    accent: ModernAccent?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit) {
    val dark = !isLightAppearance()
    val schemes = activeColorSchemes(accent)
    val scheme = remember(palette, dark, schemes) { schemes.resolve(palette, dark = dark) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(scheme.accent)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) AppTheme.colors.onSurface else AppTheme.colors.outlineVariant,
                shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label })
}

@Composable
private fun PaletteTrailingSwatch(palette: MillenniumPalette, accent: ModernAccent?) {
    val dark = !isLightAppearance()
    val schemes = activeColorSchemes(accent)
    val scheme = remember(palette, dark, schemes) { schemes.resolve(palette, dark = dark) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.accent.copy(alpha = 0.35f)))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(scheme.accent))
    }
}

@Composable
private fun PalettePreview(palette: MillenniumPalette, accent: ModernAccent?) {
    val dark = !isLightAppearance()
    val modernFonts = LocalModernFonts.current
    val config = remember(palette, accent, dark, modernFonts) {
        if (modernFonts == null) {
            MillenniumThemeConfig(palette = palette)
        } else {
            val schemes = modernColorSchemes(accent)
            val components = modernThemeComponents(schemes.resolve(palette, dark), modernFonts)
            MillenniumThemeConfig(
                colorSchemes = schemes, palette = palette,
                dark = components, light = components
            )
        }
    }
    MillenniumTheme(dark = dark, config = config) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MillenniumTheme.colors.surface)
                .border(1.dp, MillenniumTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MillenniumText(
                stringResource(Res.string.settings_preview_label),
                color = MillenniumTheme.colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MillenniumButton(onClick = {}) {
                    MillenniumText(stringResource(Res.string.settings_preview_button))
                }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Secondary) {
                    MillenniumText(stringResource(Res.string.settings_preview_secondary))
                }
            }
        }
    }
}
