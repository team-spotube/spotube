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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.isLightAppearance
import org.spotube.spotube.modules.plugin.components.PluginIconWell
import org.spotube.spotube.modules.settings.PendingThemeUi
import org.spotube.spotube.modules.settings.ThemeCardUi
import org.spotube.spotube.modules.settings.ThemeImportError
import org.spotube.spotube.modules.settings.ThemeStoreViewModel
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMagic
import spotube.composeapp.generated.resources.*

/**
 * The Themes block of Settings > Look & feel: the built-in Millennium looks and every installed
 * theme package as a card (like plugins), plus installing a new one.
 */
@Composable
internal fun ThemesPanel() {
    val viewModel = koinViewModel<ThemeStoreViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var detailsId by remember { mutableStateOf<String?>(null) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File(extensions = listOf("zip"))) { file ->
        viewModel.onFileSelected(file)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(Res.string.settings_themes_title),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
                Text(
                    stringResource(Res.string.settings_themes_subtitle),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
            MillenniumButton(onClick = { picker.launch() }) {
                Text(stringResource(Res.string.settings_themes_install))
            }
        }
        state.importError?.let { error ->
            Text(
                importErrorMessage(error, state.importErrorDetail),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.error,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .millenniumSurface(
                    background = millenniumPanelBrush(MillenniumTheme.colors),
                    shape = AppTheme.shapes.medium,
                    borderColor = MillenniumTheme.colors.border,
                    borderWidth = 1.dp,
                    shadowElevation = 6.dp,
                )
                .padding(vertical = 4.dp),
        ) {
            state.themes.forEachIndexed { index, card ->
                if (index > 0) MillenniumSeparator()
                ThemeRow(
                    card = card,
                    onOpen = { detailsId = card.id },
                    onSelect = { viewModel.select(card.id) },
                )
            }
        }
    }

    state.themes.firstOrNull { it.id == detailsId }?.let { card ->
        ThemeDetailsDialog(
            card = card,
            onDismiss = { detailsId = null },
            onSelect = { viewModel.select(card.id); detailsId = null },
            onRemove = { viewModel.remove(card.id); detailsId = null },
        )
    }
    state.pending?.let { pending ->
        InstallThemeDialog(pending, onConfirm = viewModel::confirmInstall, onCancel = viewModel::cancelInstall)
    }
}

@Composable
private fun ThemeRow(card: ThemeCardUi, onOpen: () -> Unit, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PluginIconWell(size = 40.dp) { ThemeLogo(card) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.displayName(),
                    style = AppTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                card.version?.let {
                    Text(
                        stringResource(Res.string.settings_theme_version, it),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                listOfNotNull(
                    card.author?.let { stringResource(Res.string.settings_theme_by_author, it) },
                    card.license,
                ).joinToString(" · "),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (card.selected) {
            Text(
                stringResource(Res.string.settings_theme_selected),
                style = AppTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.primary,
            )
        } else {
            MillenniumButton(onClick = onSelect, style = MillenniumButtonStyle.Secondary) {
                Text(stringResource(Res.string.settings_theme_select))
            }
        }
    }
}

/** The theme's own logo, a swatch of the accent for built-in looks, or a generic icon. */
@Composable
private fun ThemeLogo(card: ThemeCardUi, modifier: Modifier = Modifier.fillMaxSize()) {
    val logo = card.logo
    val palette = card.builtinPalette
    when {
        logo != null -> Image(logo, contentDescription = card.displayName(), contentScale = ContentScale.Crop, modifier = modifier)
        palette != null -> {
            val dark = !isLightAppearance()
            val accent = remember(palette, dark) { MillenniumColorSchemes().resolve(palette, dark).accent }
            Box(modifier.padding(8.dp).clip(CircleShape).background(accent))
        }
        else -> AppIcon(Iconsax.IconsaxMagic, contentDescription = null, tint = AppTheme.colors.primary)
    }
}

@Composable
private fun ThemeCardUi.displayName(): String = builtinPalette?.let {
    stringResource(Res.string.settings_theme_builtin_name, it.label())
} ?: name

@Composable
private fun MillenniumPalette.label(): String = when (this) {
    MillenniumPalette.Blue -> stringResource(Res.string.settings_palette_blue)
    MillenniumPalette.Orange -> stringResource(Res.string.settings_palette_orange)
    MillenniumPalette.Lime -> stringResource(Res.string.settings_palette_lime)
}

@Composable
private fun importErrorMessage(error: ThemeImportError, detail: String?): String = when (error) {
    ThemeImportError.Unreadable -> stringResource(Res.string.settings_custom_theme_error_unreadable)
    ThemeImportError.TooLarge -> stringResource(Res.string.settings_custom_theme_error_too_large)
    ThemeImportError.Corrupt -> stringResource(Res.string.settings_custom_theme_error_corrupt)
    ThemeImportError.Unsafe -> stringResource(Res.string.settings_custom_theme_error_unsafe)
    ThemeImportError.Disallowed -> stringResource(Res.string.settings_custom_theme_error_disallowed)
    ThemeImportError.InvalidTheme -> stringResource(Res.string.settings_custom_theme_import_failed)
    ThemeImportError.InvalidFont -> stringResource(Res.string.settings_custom_theme_error_font)
    ThemeImportError.InvalidImage -> stringResource(Res.string.settings_custom_theme_error_image)
    ThemeImportError.MissingMetadata -> stringResource(Res.string.settings_custom_theme_error_metadata, detail.orEmpty())
}

@Composable
private fun ThemeHeader(card: ThemeCardUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        PluginIconWell(size = 56.dp) { ThemeLogo(card) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.displayName(), style = AppTheme.typography.titleMedium, color = AppTheme.colors.onSurface)
            Text(
                listOfNotNull(
                    card.version?.let { stringResource(Res.string.settings_theme_version, it) },
                    card.author?.let { stringResource(Res.string.settings_theme_by_author, it) },
                ).joinToString(" · "),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeLinks(card: ThemeCardUi) {
    val uri = LocalUriHandler.current
    @Composable
    fun link(label: String, url: String?) {
        if (url == null) return
        Row(
            modifier = Modifier.fillMaxWidth().clickable { uri.openUri(url) },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant, modifier = Modifier.widthIn(min = 120.dp))
            Text(url.removePrefix("mailto:"), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    link(stringResource(Res.string.settings_theme_link_repository), card.repository)
    link(stringResource(Res.string.settings_theme_link_report), card.bugs)
    link(stringResource(Res.string.settings_theme_link_homepage), card.homepage)
    link(stringResource(Res.string.settings_theme_link_contact), card.contact)
}

@Composable
private fun ThemeLicense(card: ThemeCardUi) {
    if (card.license == null && card.licenseText == null) return
    var shown by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.settings_theme_license), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant, modifier = Modifier.widthIn(min = 120.dp))
            Text(card.license.orEmpty(), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
        }
        if (card.licenseText != null) {
            Text(
                stringResource(if (shown) Res.string.settings_theme_license_hide else Res.string.settings_theme_license_show),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.primary,
                modifier = Modifier.clickable { shown = !shown },
            )
            if (shown) {
                Text(
                    card.licenseText,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun ThemeContents(card: ThemeCardUi) {
    if (card.fontFiles == 0 && card.icons == 0) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.settings_theme_contents), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant, modifier = Modifier.widthIn(min = 120.dp))
        Text(stringResource(Res.string.settings_theme_contents_value, card.fontFiles, card.icons), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
    }
}

@Composable
private fun ThemeDetailsDialog(card: ThemeCardUi, onDismiss: () -> Unit, onSelect: () -> Unit, onRemove: () -> Unit) {
    MillenniumDialog(visible = true, onDismissRequest = onDismiss) {
        MillenniumDialogHeader {
            MillenniumDialogTitle(card.displayName())
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemeHeader(card)
                Text(
                    card.description ?: if (card.isBuiltin) stringResource(Res.string.settings_theme_builtin_description) else "",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                ThemeLinks(card)
                ThemeLicense(card)
                ThemeContents(card)
                if (card.problems.isNotEmpty()) {
                    Text(stringResource(Res.string.settings_theme_warnings), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurface)
                    card.problems.forEach { Text(it, style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant) }
                }
            }
        }
        MillenniumDialogFooter {
            if (!card.isBuiltin) {
                MillenniumButton(onClick = onRemove, style = MillenniumButtonStyle.Danger) {
                    Text(stringResource(Res.string.settings_theme_remove))
                }
            }
            if (!card.selected) {
                MillenniumButton(onClick = onSelect) { Text(stringResource(Res.string.settings_theme_select)) }
            } else {
                MillenniumButton(onClick = onDismiss, style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.settings_theme_close))
                }
            }
        }
    }
}

@Composable
private fun InstallThemeDialog(pending: PendingThemeUi, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val card = pending.card
    MillenniumDialog(visible = true, onDismissRequest = onCancel) {
        MillenniumDialogHeader {
            MillenniumDialogTitle(stringResource(Res.string.settings_theme_install_title, card.name))
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemeHeader(card)
                card.description?.let {
                    Text(it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
                }
                if (pending.replaces) {
                    Text(
                        pending.replacesVersion?.let { stringResource(Res.string.settings_theme_install_replaces, it) }
                            ?: stringResource(Res.string.settings_theme_install_replaces_unknown),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurface,
                    )
                }
                ThemeLinks(card)
                ThemeLicense(card)
                ThemeContents(card)
                Text(stringResource(Res.string.settings_theme_install_size, pending.sizeKb), style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
                Text(stringResource(Res.string.settings_theme_install_warning), style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
            }
        }
        MillenniumDialogFooter {
            MillenniumButton(onClick = onCancel, style = MillenniumButtonStyle.Secondary) {
                Text(stringResource(Res.string.common_cancel))
            }
            MillenniumButton(onClick = onConfirm) {
                Text(stringResource(if (pending.replaces) Res.string.settings_theme_install_update else Res.string.settings_theme_install_confirm))
            }
        }
    }
}
