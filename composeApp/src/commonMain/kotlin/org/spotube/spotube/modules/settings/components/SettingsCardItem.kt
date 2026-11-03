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

package org.spotube.spotube.modules.settings.components

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumListTile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable setting card item component for consistent styling
 * across all list items in the settings screen
 */
@Composable
fun SettingCardItem(
    enabled: Boolean = true,
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    MillenniumListTile(
        selected = false,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        leading = icon,
        title = {
            Text(
                title,
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.onSurface
            )
        },
        subtitle = subtitle?.let {
            {
                Text(
                    it,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        },
        trailing = trailingContent)
}
