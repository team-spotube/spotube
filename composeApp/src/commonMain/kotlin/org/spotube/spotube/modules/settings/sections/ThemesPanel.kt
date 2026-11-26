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

import org.spotube.spotube.core.ui.theming.appPanelBrush
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.millennium.composables.MillenniumSwitch
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.spotube.core.ui.theming.engine.ThemeVarType
import org.spotube.spotube.modules.settings.ThemeParamUi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.resources.iconsax.IconsaxAdd
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.isLightAppearance
import org.spotube.spotube.modules.plugin.components.PluginIconWell
import org.spotube.spotube.core.ui.theming.engine.ThemeInstallKind
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

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionLabel(
            title = Res.string.settings_themes_title,
            trailing = {
                val status = when {
                    state.checkingUpdates -> Res.string.settings_themes_checking_updates
                    state.updateCheckFailed -> Res.string.settings_themes_update_check_failed
                    state.upToDate -> Res.string.settings_themes_up_to_date
                    else -> null
                }
                status?.let {
                    Text(stringResource(it), style = AppTheme.typography.labelSmall, color = AppTheme.colors.onSurfaceVariant)
                }
                MillenniumIconButton(
                    icon = Iconsax.IconsaxRefreshRight,
                    contentDescription = stringResource(Res.string.settings_themes_check_updates),
                    onClick = viewModel::checkUpdates,
                    style = MillenniumIconButtonStyle.Quiet,
                    enabled = !state.checkingUpdates,
                )
                // The main action of the page: a filled, larger button so it is easy to see and hit.
                MillenniumIconButton(
                    icon = Iconsax.IconsaxAdd,
                    contentDescription = stringResource(Res.string.settings_themes_install),
                    onClick = { picker.launch() },
                    style = MillenniumIconButtonStyle.Primary,
                    iconSize = 24.dp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
        )
        state.importError?.let { error ->
            Text(
                importErrorMessage(error, state.importErrorDetail),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        ThemeCardList {
            state.themes.forEachIndexed { index, card ->
                if (index > 0) MillenniumSeparator()
                ThemeRow(
                    card = card,
                    onOpen = { detailsId = card.id },
                    onUpdate = { viewModel.updateTheme(card.id) },
                )
            }
        }
        ThemeDiscoverSection(
            state = state.discover,
            onOpen = viewModel::openRepo,
            onRetry = viewModel::retryDiscover,
            onLoadMore = viewModel::loadMoreDiscover,
        )
    }

    ThemeReleasesDialog(state.discover, onInstall = viewModel::installAsset, onDismiss = viewModel::closeRepo)

    state.themes.firstOrNull { it.id == detailsId }?.let { card ->
        ThemeDetailsDialog(
            card = card,
            onDismiss = { detailsId = null },
            onSelect = { viewModel.select(card.id); detailsId = null },
            onRemove = { viewModel.remove(card.id); detailsId = null },
            onSetParameter = { name, value -> viewModel.setParameter(card.id, name, value) },
            onResetParameters = { viewModel.resetParameters(card.id) },
        )
    }
    state.pending?.let { pending ->
        InstallThemeDialog(pending, onConfirm = viewModel::confirmInstall, onCancel = viewModel::cancelInstall)
    }
}

/** The rounded panel every list on this page sits in, inset like the other settings cards. */
@Composable
internal fun ThemeCardList(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .millenniumSurface(
                background = appPanelBrush(),
                shape = AppTheme.shapes.medium,
                borderColor = MillenniumTheme.colors.border,
                borderWidth = 1.dp,
                shadowElevation = 6.dp,
            )
            .padding(vertical = 4.dp),
    ) { content() }
}

/**
 * One theme row. Built on [MillenniumListTile] so hover, press, focus and the selected look come from the active
 * theme (a bare `clickable` draws nothing: Millennium turns Compose's default indication off).
 */
@Composable
internal fun ThemeRow(card: ThemeCardUi, onOpen: () -> Unit, onUpdate: () -> Unit = {}) {
    MillenniumListTile(
        modifier = Modifier.fillMaxWidth(),
        selected = card.selected,
        // Opening the details is the row's action; selecting happens from the dialog's Select button.
        onClick = onOpen,
        leading = { PluginIconWell(size = 40.dp) { ThemeLogo(card) } },
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.displayName(),
                    style = AppTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
        },
        subtitle = card.author?.let { author ->
            {
                Text(
                    stringResource(Res.string.settings_theme_by_author, author),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                card.updateVersion?.let { version ->
                    MillenniumButton(onClick = onUpdate) {
                        Text(stringResource(Res.string.settings_theme_update_available, version))
                    }
                }
            }
        },
    )
}

/** The theme's own logo, a swatch of the accent for built-in looks, or a generic icon. */
@Composable
private fun ThemeLogo(card: ThemeCardUi, modifier: Modifier = Modifier.fillMaxSize()) {
    val logo = card.logo
    when {
        logo != null -> Image(logo, contentDescription = card.displayName(), contentScale = ContentScale.Crop, modifier = modifier)
        card.isModern -> Box(modifier.padding(10.dp).clip(RoundedCornerShape(5.dp)).background(AppTheme.colors.onSurface))
        card.swatchColor != null -> Box(modifier.padding(8.dp).clip(CircleShape).background(parseSwatch(card.swatchColor)))
        else -> AppIcon(Iconsax.IconsaxMagic, contentDescription = null, tint = AppTheme.colors.primary)
    }
}

@Composable
private fun ThemeCardUi.displayName(): String = when {
    isModern -> stringResource(Res.string.settings_theme_modern_name)
    isMillennium -> stringResource(Res.string.settings_theme_millennium_name)
    else -> name
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

/** One label/value line of the theme info: the label column has a fixed width so every value lines up. */
@Composable
private fun InfoRow(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant, modifier = Modifier.width(88.dp))
        Box(Modifier.weight(1f)) { value() }
    }
}

/** `https://github.com/owner/repo` -> `github.com/owner/repo`: shorter, and nothing is cut off. */
private fun String.displayUrl() = removePrefix("mailto:").removePrefix("https://").removePrefix("http://").trimEnd('/')

@Composable
private fun ThemeLinks(card: ThemeCardUi) {
    val uri = LocalUriHandler.current
    @Composable
    fun link(label: String, url: String?) {
        if (url == null) return
        InfoRow(label, Modifier.clickable { uri.openUri(url) }) {
            Text(url.displayUrl(), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    InfoRow(stringResource(Res.string.settings_theme_license)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            card.license?.let { Text(it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface) }
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
}

@Composable
private fun ThemeContents(card: ThemeCardUi, sizeKb: Int? = null) {
    if (card.fontFiles == 0 && card.icons == 0 && sizeKb == null) return
    InfoRow(stringResource(Res.string.settings_theme_contents)) {
        Text(
            listOfNotNull(
                stringResource(Res.string.settings_theme_contents_value, card.fontFiles, card.icons).takeIf { card.fontFiles > 0 || card.icons > 0 },
                sizeKb?.let { stringResource(Res.string.settings_theme_install_size, it) },
            ).joinToString(" · "),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Composable
private fun ThemeDetailsDialog(
    card: ThemeCardUi,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onSetParameter: (String, String) -> Unit,
    onResetParameters: () -> Unit,
) {
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
                    card.description ?: when {
                        card.isModern -> stringResource(Res.string.settings_theme_modern_description)
                        card.isBuiltin -> stringResource(Res.string.settings_theme_builtin_description)
                        else -> ""
                    },
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                ThemeCustomize(card, onSetParameter, onResetParameters)
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
            MillenniumDialogTitle(
                when (pending.kind) {
                    ThemeInstallKind.Install -> stringResource(Res.string.settings_theme_install_title, card.name)
                    ThemeInstallKind.Update -> stringResource(Res.string.settings_theme_update_title, card.name)
                    else -> stringResource(Res.string.settings_theme_replace_title, card.name)
                },
            )
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
                installRelationMessage(pending)?.let {
                    Text(it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                }
                ThemeLinks(card)
                ThemeLicense(card)
                ThemeContents(card, sizeKb = pending.sizeKb)
                Text(stringResource(Res.string.settings_theme_install_warning), style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
            }
        }
        MillenniumDialogFooter {
            MillenniumButton(onClick = onCancel, style = MillenniumButtonStyle.Secondary) {
                Text(stringResource(Res.string.common_cancel))
            }
            MillenniumButton(onClick = onConfirm) {
                Text(
                    stringResource(
                        when (pending.kind) {
                            ThemeInstallKind.Install -> Res.string.settings_theme_install_confirm
                            ThemeInstallKind.Update -> Res.string.settings_theme_install_update
                            else -> Res.string.settings_theme_install_replace
                        },
                    ),
                )
            }
        }
    }
}

/** The theme's own settings (accent, radius, density...), applied live to the selected theme. */
@Composable
internal fun ThemeCustomize(card: ThemeCardUi, onSet: (String, String) -> Unit, onReset: () -> Unit) {
    if (card.parameters.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.settings_theme_customize), style = AppTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
            if (card.hasCustomParameters) {
                Text(
                    stringResource(Res.string.settings_theme_customize_reset),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.primary,
                    modifier = Modifier.clickable(onClick = onReset),
                )
            }
        }
        if (!card.selected) {
            Text(
                stringResource(Res.string.settings_theme_customize_unselected),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        card.parameters.forEach { p -> ThemeParameterControl(p) { onSet(p.name, it) } }
    }
}

@Composable
private fun ThemeParameterControl(p: ThemeParamUi, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (p.type) {
            // The switch draws its own label.
            ThemeVarType.Bool -> MillenniumSwitch(checked = p.value == "true", onCheckedChange = { onChange(it.toString()) }, label = p.label)
            ThemeVarType.Number -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.label, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                    Text(p.value, style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant)
                }
                val steps = (((p.max - p.min) / p.step).toInt() - 1).coerceAtLeast(0)
                MillenniumSlider(
                    value = (p.value.toFloatOrNull() ?: p.min).coerceIn(p.min, p.max),
                    onValueChange = { onChange(it.toString()) },
                    label = p.label,
                    valueRange = p.min..p.max,
                    steps = steps,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ThemeVarType.Enum -> {
                Text(p.label, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                if (p.options.any { it.color != null }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        p.options.forEach { o -> ColorSwatch(o.color ?: "#888888", o.label, selected = o.value == p.value) { onChange(o.value) } }
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        p.options.forEach { o ->
                            MillenniumButton(
                                onClick = { onChange(o.value) },
                                style = if (o.value == p.value) MillenniumButtonStyle.Primary else MillenniumButtonStyle.Secondary,
                            ) { Text(o.label) }
                        }
                    }
                }
            }
            ThemeVarType.Color -> {
                Text(p.label, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                if (p.presets.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        p.presets.forEach { c -> ColorSwatch(c, c, selected = c.equals(p.value, ignoreCase = true)) { onChange(c) } }
                    }
                }
                val field = rememberTextFieldState(p.value)
                LaunchedEffect(field) {
                    snapshotFlow { field.text.toString() }.collect { if (it != p.value) onChange(it) }
                }
                MillenniumTextField(
                    state = field,
                    placeholder = stringResource(Res.string.settings_theme_color_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ThemeVarType.String -> {
                Text(p.label, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                val field = rememberTextFieldState(p.value)
                LaunchedEffect(field) {
                    snapshotFlow { field.text.toString() }.collect { if (it != p.value) onChange(it) }
                }
                MillenniumTextField(state = field, modifier = Modifier.fillMaxWidth())
            }
        }
        p.description?.let {
            Text(it, style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val color = remember(hex) { parseSwatch(hex) }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) AppTheme.colors.onSurface else AppTheme.colors.outlineVariant,
                shape = CircleShape,
            )
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
    )
}

private fun parseSwatch(hex: String): androidx.compose.ui.graphics.Color {
    val h = hex.removePrefix("#")
    val v = h.toLongOrNull(16) ?: return androidx.compose.ui.graphics.Color.Gray
    return if (h.length == 6) androidx.compose.ui.graphics.Color(0xFF000000L or v) else androidx.compose.ui.graphics.Color.Gray
}

/** The sentence explaining how the package relates to what is installed (null for a fresh install). */
@Composable
private fun installRelationMessage(pending: PendingThemeUi): String? {
    val installed = pending.installedVersion.orEmpty()
    val incoming = pending.incomingVersion.orEmpty()
    return when (pending.kind) {
        ThemeInstallKind.Install -> null
        ThemeInstallKind.Update -> stringResource(Res.string.settings_theme_update_message, installed, incoming)
        ThemeInstallKind.SameVersion -> stringResource(Res.string.settings_theme_replace_same_version, incoming)
        ThemeInstallKind.Downgrade -> stringResource(Res.string.settings_theme_replace_downgrade, installed, incoming)
        ThemeInstallKind.InvalidIncomingVersion -> stringResource(Res.string.settings_theme_replace_invalid_incoming, incoming)
        ThemeInstallKind.InvalidInstalledVersion -> stringResource(Res.string.settings_theme_replace_invalid_installed, installed, incoming)
    }
}
