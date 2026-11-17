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

package org.spotube.spotube.core.ui.theming

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconTheme
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.engine.ThemeIcon

/** Icon replacements from the active JSON theme, keyed by the app icon's vector name. */
val LocalThemeIcons = compositionLocalOf<Map<String, ThemeIcon>> { emptyMap() }

/**
 * Drop-in replacement for `MillenniumIcon` that honours theme icon overrides.
 *
 * With no override it is exactly [MillenniumIcon]. An override (an icon-font glyph or a bundled
 * PNG) is drawn in the same box, with the same size, alpha and tint rules.
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    state: MillenniumControlState = MillenniumControlState.Default,
    size: Dp? = null,
    alpha: Float? = null,
    tintAnimationMillis: Int? = null,
    theme: MillenniumIconTheme = MillenniumTheme.iconTheme,
) {
    val override = LocalThemeIcons.current[imageVector.name]
    if (override == null) {
        MillenniumIcon(imageVector, contentDescription, modifier, tint, state, size, alpha, tintAnimationMillis, theme)
        return
    }
    val properties = theme.states.resolve(state)
    val colors = MillenniumTheme.colors
    val resolvedTint = tint ?: properties.tint ?: when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused, MillenniumControlState.Selected -> colors.accent
        MillenniumControlState.Pressed -> colors.accentBottom
        MillenniumControlState.Disabled -> colors.muted
        MillenniumControlState.Error -> colors.danger
        MillenniumControlState.Default -> colors.text
    }
    val box = size ?: properties.size
    Box(
        modifier = modifier
            .size(box)
            .alpha(alpha ?: properties.alpha)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        when (override) {
            is ThemeIcon.Glyph -> {
                val fontSize = with(LocalDensity.current) { box.toSp() } * 0.9f
                BasicText(
                    text = override.text,
                    style = TextStyle(fontFamily = override.family, fontSize = fontSize, color = resolvedTint),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            is ThemeIcon.Bitmap -> Image(
                bitmap = override.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (override.tinted) ColorFilter.tint(resolvedTint) else null,
            )
        }
    }
}
