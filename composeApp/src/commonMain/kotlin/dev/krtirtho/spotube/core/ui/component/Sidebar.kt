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

package dev.krtirtho.spotube.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.SecondaryButton
import dev.krtirtho.spotube.core.ui.base.copyPadding
import dev.krtirtho.spotube.core.ui.base.copyShape

/**
 * The shared navigation item used by every sidebar in the app (the main app sidebar
 * and the settings sidebar), so they stay visually identical.
 *
 * @param icon leading icon of the item.
 * @param badge optional overlay drawn on the icon (for example a download indicator).
 */
@Composable
fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val itemContent: @Composable RowScope.() -> Unit = {
        Box(modifier = Modifier.size(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            badge?.invoke(this)
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )
        }
        Spacer(modifier = if (expanded) Modifier.weight(1f) else Modifier)
    }

    val buttonModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 7.dp, vertical = 2.dp)
    val contentPadding = PaddingValues(horizontal = 5.dp)

    if (selected) {
        SecondaryButton(
            onClick = onClick,
            modifier = buttonModifier,
            theme = LocalBaseUITheme.current.buttons.secondary
                .copyPadding(contentPadding)
                .copyShape(RoundedCornerShape(5.dp)),
            content = itemContent,
        )
    } else {
        OutlineButton(
            onClick = onClick,
            modifier = buttonModifier,
            theme = LocalBaseUITheme.current.buttons.outline
                .copyPadding(contentPadding)
                .copyShape(RoundedCornerShape(5.dp)),
            hoverOnly = true,
            content = itemContent,
        )
    }
}

/**
 * The shared section heading used above groups of [SidebarItem]s.
 */
@Composable
fun SidebarSectionLabel(
    text: String,
    expanded: Boolean = true,
) {
    AnimatedVisibility(visible = expanded) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}
