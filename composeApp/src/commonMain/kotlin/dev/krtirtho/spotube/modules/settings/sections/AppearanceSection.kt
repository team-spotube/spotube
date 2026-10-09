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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.core.ui.base.BaseSurface
import dev.krtirtho.spotube.core.ui.base.LocalBaseUIColors
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.SurfaceRole
import dev.krtirtho.spotube.core.ui.base.SurfaceStyle
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.core.ui.base.baseUIColorScheme
import dev.krtirtho.spotube.modules.settings.AccentColors
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.Theme
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.colorFor
import dev.krtirtho.spotube.modules.settings.components.SelectionSettingCard
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.modules.settings.recommendedAccents
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxColorSwatch
import dev.krtirtho.spotube.resources.iconsax.IconsaxMagic
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

internal fun LazyListScope.appearanceSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true,
) {
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
                // Surface material and accent are one decision: each surface only offers the
                // accents that are known to pair with it, so the result is always a curated,
                // good-looking combination instead of an arbitrary one.
                AppearanceSettingCard(
                    selectedSurface = settings.surfaceTheme,
                    selectedAccent = settings.accentColor,
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxMagic,
                            stringResource(Res.string.settings_appearance_title)
                        )
                    },
                    onSaved = { surface, accent ->
                        settingsViewModel.updateSettings {
                            copy(surfaceTheme = surface, accentColor = accent)
                        }
                    },
                )
            },
        )
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
private fun SurfaceStyle.displayLabel(): String {
    return when (this) {
        SurfaceStyle.Watery -> stringResource(Res.string.settings_surface_watery)
        SurfaceStyle.Icy -> stringResource(Res.string.settings_surface_icy)
        SurfaceStyle.Chrome -> stringResource(Res.string.settings_surface_chrome)
        SurfaceStyle.Acrylic -> stringResource(Res.string.settings_surface_acrylic)
        SurfaceStyle.Glass -> stringResource(Res.string.settings_surface_glass)
    }
}

@Composable
private fun AccentColors.displayLabel(): String {
    return when (this) {
        AccentColors.GREEN_GOBLIN -> stringResource(Res.string.settings_accent_green_goblin)
        AccentColors.ELECTRIC_VIOLET -> stringResource(Res.string.settings_accent_electric_violet)
        AccentColors.OCEANIC_CYAN -> stringResource(Res.string.settings_accent_oceanic_cyan)
        AccentColors.SUNSET_ORANGE -> stringResource(Res.string.settings_accent_sunset_orange)
        AccentColors.ROSE_GARDEN -> stringResource(Res.string.settings_accent_rose_garden)
        AccentColors.MIDNIGHT_BLUE -> stringResource(Res.string.settings_accent_midnight_blue)
        AccentColors.METALLIC_SLATE -> stringResource(Res.string.settings_accent_metallic_slate)
    }
}

@Composable
private fun AppearanceSettingCard(
    selectedSurface: SurfaceStyle,
    selectedAccent: AccentColors,
    icon: (@Composable () -> Unit)? = null,
    onSaved: (SurfaceStyle, AccentColors) -> Unit,
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val isLight = LocalBaseUIColors.current.isLight

    SettingCardItem(
        title = stringResource(Res.string.settings_appearance_title),
        subtitle = stringResource(
            Res.string.settings_appearance_subtitle_current,
            selectedSurface.displayLabel(),
            selectedAccent.displayLabel(),
        ),
        icon = icon,
        trailingContent = {
            AppearanceTrailingSwatch(
                surface = selectedSurface,
                accent = selectedAccent,
                isLight = isLight,
            )
        },
        onClick = { isDialogOpen = true },
    )

    if (isDialogOpen) {
        var draftSurface by remember(selectedSurface, isDialogOpen) {
            mutableStateOf(selectedSurface)
        }
        var draftAccent by remember(selectedSurface, selectedAccent, isDialogOpen) {
            mutableStateOf(selectedAccent)
        }

        ThemedDialog(
            onDismissRequest = { isDialogOpen = false },
            title = {
                Text(
                    stringResource(Res.string.settings_appearance_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            actions = {
                PrimaryButton(
                    onClick = {
                        onSaved(draftSurface, draftAccent)
                        isDialogOpen = false
                    }
                ) {
                    Text(stringResource(Res.string.settings_action_save))
                }
                OutlineButton(onClick = { isDialogOpen = false }) {
                    Text(stringResource(Res.string.settings_action_cancel))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(Res.string.settings_appearance_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(Res.string.settings_appearance_surface_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SurfaceStyle.entries.forEach { style ->
                        SurfaceStyleTile(
                            style = style,
                            accent = draftAccent,
                            isLight = isLight,
                            selected = style == draftSurface,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                draftSurface = style
                                if (draftAccent !in style.recommendedAccents) {
                                    draftAccent = style.recommendedAccents.first()
                                }
                            },
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.settings_appearance_accent_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    draftSurface.recommendedAccents.forEach { accent ->
                        AccentSwatch(
                            accent = accent,
                            label = accent.displayLabel(),
                            isLight = isLight,
                            selected = accent == draftAccent,
                            onClick = { draftAccent = accent },
                        )
                    }
                }

                AppearancePreview(
                    surface = draftSurface,
                    accent = draftAccent,
                    isLight = isLight,
                )
            }
        }
    }
}

@Composable
private fun SurfaceStyleTile(
    style: SurfaceStyle,
    accent: AccentColors,
    isLight: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = remember(style, accent, isLight) {
        baseUIColorScheme(style, isLight, accent.colorFor(isLight))
    }
    Box(
        modifier = modifier
            .height(70.dp)
            .then(
                if (selected) {
                    Modifier
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        .padding(2.dp)
                } else {
                    Modifier.padding(2.dp)
                }
            ),
    ) {
        CompositionLocalProvider(LocalBaseUIColors provides scheme) {
            BaseSurface(
                style = style,
                role = SurfaceRole.PrimaryBackground,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick),
                contentPadding = PaddingValues(8.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = style.displayLabel(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(accent.colorFor(isLight), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: AccentColors,
    label: String,
    isLight: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accent.colorFor(isLight))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    )
}

@Composable
private fun AppearanceTrailingSwatch(
    surface: SurfaceStyle,
    accent: AccentColors,
    isLight: Boolean,
) {
    val scheme = remember(surface, accent, isLight) {
        baseUIColorScheme(surface, isLight, accent.colorFor(isLight))
    }
    CompositionLocalProvider(LocalBaseUIColors provides scheme) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaseSurface(
                style = surface,
                role = SurfaceRole.PrimaryBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(width = 46.dp, height = 28.dp),
            ) {}
            BaseSurface(
                style = surface,
                role = SurfaceRole.PrimaryAccent,
                shape = CircleShape,
                modifier = Modifier.size(18.dp),
            ) {}
        }
    }
}

@Composable
private fun AppearancePreview(
    surface: SurfaceStyle,
    accent: AccentColors,
    isLight: Boolean,
) {
    val scheme = remember(surface, accent, isLight) {
        baseUIColorScheme(surface, isLight, accent.colorFor(isLight))
    }
    CompositionLocalProvider(LocalBaseUIColors provides scheme) {
        BaseSurface(
            style = surface,
            role = SurfaceRole.PrimaryBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_preview_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BaseSurface(
                        style = surface,
                        role = SurfaceRole.PrimaryAccent,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.settings_preview_button),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    BaseSurface(
                        style = surface,
                        role = SurfaceRole.SecondaryAccent,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.settings_preview_secondary),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                BaseSurface(
                    style = surface,
                    role = SurfaceRole.SecondaryBackground,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_preview_label),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
