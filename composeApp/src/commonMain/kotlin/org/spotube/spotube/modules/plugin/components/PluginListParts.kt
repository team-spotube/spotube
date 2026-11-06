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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTooltip
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.plugin.PluginAbility

// Shared pieces of the plugin manager lists: glossy section headers, the pearl list panel,
// icon wells, gel badges and pills. Kept together so installed and discovered plugins read
// as one family with the settings screens.

/** Row content narrower than this moves trailing actions below the text instead of beside it. */
internal val PluginRowCompactWidth = 520.dp

/** A plain section title with an optional trailing action. */
@Composable
internal fun PluginSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        action?.invoke()
    }
}

/** Small square of accent gel holding an icon, like a 2007 toolbar glyph. */
@Composable
internal fun GelIconTile(icon: ImageVector, size: Dp = 34.dp) {
    val colors = MillenniumTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .millenniumSurface(
                background = gelBrush(colors.accentTop, colors.accentBottom),
                shape = RoundedCornerShape(size / 3.4f),
                borderColor = colors.accentBottom.copy(alpha = 0.7f),
                borderWidth = 1.dp,
                shadowElevation = 2.dp,
                innerGlowColor = Color.White.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.52f),
            tint = colors.onAccent)
    }
}

/** The pearl panel that groups rows, matching the settings section cards. */
@Composable
internal fun PluginListPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .millenniumSurface(
                background = millenniumPanelBrush(MillenniumTheme.colors),
                shape = AppTheme.shapes.medium,
                borderColor = MillenniumTheme.colors.border,
                borderWidth = 1.dp,
                shadowElevation = 6.dp,
                innerGlowColor = MillenniumTheme.colors.highlight.copy(alpha = 0.55f)),
        content = content)
}

@Composable
internal fun PluginRowSeparator() {
    MillenniumSeparator(modifier = Modifier.padding(horizontal = 14.dp))
}

/** A bevelled well that frames a plugin logo, so round and square logos sit alike. */
@Composable
internal fun PluginIconWell(
    size: Dp = 48.dp,
    content: @Composable () -> Unit) {
    val colors = MillenniumTheme.colors
    val shape = RoundedCornerShape(size / 4f)
    Box(
        modifier = Modifier
            .size(size)
            .millenniumSurface(
                background = millenniumBevelBrush(colors),
                shape = shape,
                borderColor = colors.border,
                borderWidth = 1.dp,
                shadowElevation = 2.dp)
            .padding(1.dp)
            .clip(shape),
        contentAlignment = Alignment.Center) {
        content()
    }
}

/** Glossy accent badge, used for the "Active" and "Official" states. */
@Composable
internal fun GelBadge(text: String, icon: ImageVector? = null) {
    val colors = MillenniumTheme.colors
    Row(
        modifier = Modifier
            .millenniumSurface(
                background = gelBrush(colors.accentTop, colors.accentBottom),
                shape = RoundedCornerShape(50),
                borderColor = colors.accentBottom.copy(alpha = 0.7f),
                borderWidth = 1.dp,
                innerGlowColor = Color.White.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (icon != null) {
            MillenniumIcon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = colors.onAccent)
        }
        Text(
            text,
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onAccent,
            maxLines = 1)
    }
}

/** Quiet outlined tag for secondary states such as "Built-in". */
@Composable
internal fun OutlineTag(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MillenniumTheme.colors.inset)
            .border(1.dp, MillenniumTheme.colors.border, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (icon != null) {
            MillenniumIcon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = AppTheme.colors.onSurfaceVariant)
        }
        Text(
            text,
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.onSurfaceVariant,
            maxLines = 1)
    }
}

/** Ability tag tinted with the ability's accent (the same hue as its default-plugin tile). */
@Composable
internal fun AbilityTag(ability: PluginAbility) {
    val accent = ability.accentColor()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MillenniumIcon(ability.abilityIcon(), contentDescription = null, modifier = Modifier.size(11.dp), tint = accent)
        Text(
            ability.displayLabel(),
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.onSurface,
            maxLines = 1)
    }
}

/** Icon + muted label, for author / version / stars metadata. */
@Composable
internal fun MetaLabel(text: String, icon: ImageVector? = null, iconDescription: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (icon != null) {
            MillenniumIcon(
                icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(12.dp),
                tint = AppTheme.colors.onSurfaceVariant)
        }
        Text(
            text,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

/** Quiet icon button with a hover tooltip carrying its label. */
@Composable
internal fun PluginActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    loading: Boolean = false) {
    MillenniumTooltip(text = label) {
        MillenniumIconButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            loading = loading,
            style = MillenniumIconButtonStyle.Quiet)
    }
}

/** Fills the logo well with the logo, or a tinted fallback icon. */
@Composable
internal fun PluginLogoFallback(icon: ImageVector, tint: Color = AppTheme.colors.primary) {
    Box(
        modifier = Modifier.fillMaxSize().background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center) {
        MillenniumIcon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}
