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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Monitor
import spotube.composeapp.generated.resources.*
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.Radio
import dev.krtirtho.spotube.modules.settings.AccentColors
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.Theme
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SelectionSettingCard
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxColorSwatch
import dev.krtirtho.spotube.resources.iconsax.IconsaxColorsSquare
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
                AccentColorSettingCard(
                    selectedAccent = settings.accentColor,
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxColorsSquare, stringResource(Res.string.settings_accent_title))
                    },
                    onColorSaved = { accent ->
                        settingsViewModel.updateSettings {
                            copy(accentColor = accent)
                        }
                    }
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
private fun AccentColorSettingCard(
    selectedAccent: AccentColors,
    icon: (@Composable () -> Unit)? = null,
    onColorSaved: (AccentColors) -> Unit,
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    SettingCardItem(
        title = stringResource(Res.string.settings_accent_title),
        subtitle = stringResource(
            Res.string.settings_accent_subtitle_current,
            selectedAccent.displayLabel()
        ),
        icon = icon,
        trailingContent = {
            AccentDualPreview(
                lightAccent = selectedAccent.toLightColor(),
                darkAccent = selectedAccent.toDarkColor(),
            )
        },
        onClick = {
            isDialogOpen = true
        }
    )

    if (isDialogOpen) {
        var draftAccent by remember(selectedAccent, isDialogOpen) { mutableStateOf(selectedAccent) }

        ThemedDialog(
            onDismissRequest = { isDialogOpen = false },
            title = {
                Text(stringResource(Res.string.settings_accent_dialog_title), style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                PrimaryButton(
                    onClick = {
                        onColorSaved(draftAccent)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.settings_accent_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccentThemePreview(
                        title = stringResource(Res.string.settings_preview_light),
                        accent = draftAccent.toLightColor(),
                        background = Color(0xFFFFFFFF),
                        textColor = Color(0xFF121212),
                        modifier = Modifier.weight(1f)
                    )
                    AccentThemePreview(
                        title = stringResource(Res.string.settings_preview_dark),
                        accent = draftAccent.toDarkColor(),
                        background = Color(0xFF121212),
                        textColor = Color(0xFFEDEDED),
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccentColors.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draftAccent = option }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Radio(
                                selected = option == draftAccent,
                                onClick = { draftAccent = option }
                            )
                            Text(
                                text = option.displayLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            AccentDualPreview(
                                lightAccent = option.toLightColor(),
                                darkAccent = option.toDarkColor(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentDualPreview(
    lightAccent: Color,
    darkAccent: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(1.dp, Color(0x22000000), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(lightAccent, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFF121212), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(darkAccent, CircleShape)
            )
        }
    }
}

@Composable
private fun AccentThemePreview(
    title: String,
    accent: Color,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, textColor.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = textColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(accent, RoundedCornerShape(999.dp))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(accent, CircleShape)
            )
            Text(
                stringResource(Res.string.settings_preview_label),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                stringResource(Res.string.settings_preview_button),
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}
