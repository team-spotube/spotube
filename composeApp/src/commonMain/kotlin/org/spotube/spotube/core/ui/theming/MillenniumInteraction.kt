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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlState
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmStatic

/**
 * Switch for Millennium's built-in interaction gel: the white sheen and rim it paints over hovered
 * and focused controls, the shade over pressed ones, and the accent gradient of hovered list rows.
 * None of it reads the theme.
 *
 * The build wraps the library functions that produce it (see `MillenniumInteractionPatch` in
 * buildSrc), on desktop and Android, so their results pass through the same-named functions here.
 * [SpotubeTheme] turns the gel off for the Modern style, whose components carry their own flat
 * hover and press fills. Keep these names and parameter lists in step with that patch.
 */
object MillenniumInteraction {
    @Volatile
    var glossEnabled: Boolean = true

    private val noLevels: Pair<Float, Float> = 0f to 0f

    /** (hover/focus, press) strength of the gel for a control state. */
    @JvmStatic
    fun millenniumInteractionLevels(levels: Pair<Float, Float>, state: MillenniumControlState): Pair<Float, Float> =
        if (glossEnabled) levels else noLevels

    /** A list row's fill. Modern: the same faint flat wash as the rest of its controls. */
    @JvmStatic
    fun listTileBackground(
        background: Brush,
        colors: MillenniumColors,
        selected: Boolean,
        state: MillenniumControlState,
        gelStrength: Float
    ): Brush {
        if (glossEnabled || selected) return background
        return SolidColor(
            when (state) {
                MillenniumControlState.Hovered, MillenniumControlState.Focused -> colors.text.copy(alpha = 0.04f)
                MillenniumControlState.Pressed -> colors.text.copy(alpha = 0.07f)
                else -> Color.Transparent
            }
        )
    }
}
