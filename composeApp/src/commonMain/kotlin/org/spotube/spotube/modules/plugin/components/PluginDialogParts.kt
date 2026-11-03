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

package org.spotube.spotube.modules.plugin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.plugin.PluginEntry
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxBoxAdd
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import okio.Path
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface

// Shared building blocks for the plugin dialogs/sheets so they all read as one family and
// match the panel/tile language used by the plugin cards and the rest of the app.

/** Body column for plugin dialogs; the adaptive dialog/sheet around it handles scrolling. */
@Composable
internal fun PluginDialogBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content)
}

/** Title for the phone bottom sheet; the desktop dialog shows the same text in its titlebar. */
@Composable
internal fun PluginDialogTitle(
    title: String,
    icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) {
            PluginIconTile(icon, size = 40.dp)
        }
        Text(
            title,
            style = AppTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.onSurface)
    }
}

/** Rounded, softly bordered panel — the same surface the plugin cards use. */
@Composable
internal fun PluginPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .millenniumSurface(
                background = millenniumPanelBrush(MillenniumTheme.colors),
                shape = RoundedCornerShape(14.dp),
                borderColor = AppTheme.colors.outlineVariant,
                borderWidth = 1.dp),
        content = content)
}

@Composable
internal fun PluginIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    tint: Color = AppTheme.colors.primary) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.4f))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = tint)
    }
}

@Composable
internal fun PluginLogoTile(
    plugin: PluginEntry,
    logoPath: Path?,
    size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.4f))
            .background(AppTheme.colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center) {
        PluginLogo(
            plugin = plugin,
            logoPath = logoPath,
            contentDescription = plugin.name,
            modifier = Modifier.fillMaxSize(),
            fallback = {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxBoxAdd,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(size * 0.5f))
            })
    }
}

@Composable
internal fun PluginPill(
    text: String,
    icon: ImageVector? = null,
    tint: Color = AppTheme.colors.onSurfaceVariant) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (icon != null) {
            MillenniumIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = tint)
        }
        Text(
            text,
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

/** Tinted info strip; [isWarning] switches it to the error tone. */
@Composable
internal fun PluginCallout(text: String, isWarning: Boolean = false) {
    val tint = if (isWarning) AppTheme.colors.error else AppTheme.colors.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MillenniumIcon(
            imageVector = Iconsax.IconsaxInformation,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tint)
        Text(
            text,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurface)
    }
}

@Composable
internal fun PluginSectionLabel(text: String, badge: String? = null) {
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text,
            style = AppTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.onSurface)
        if (badge != null) {
            PluginPill(badge, tint = AppTheme.colors.primary)
        }
    }
}

/** Equal-width secondary + primary action pair. */
@Composable
internal fun PluginDialogActions(
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MillenniumButton(
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
            style = if (primaryLabel != null) MillenniumButtonStyle.Secondary else MillenniumButtonStyle.Primary) {
            Text(secondaryLabel)
        }
        if (primaryLabel != null && onPrimary != null) {
            MillenniumButton(
                onClick = onPrimary,
                enabled = primaryEnabled,
                loading = primaryLoading,
                modifier = Modifier.weight(1f),
                style = MillenniumButtonStyle.Primary) {
                Text(primaryLabel)
            }
        }
    }
}
