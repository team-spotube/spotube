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

package org.spotube.spotube.core.ui.theming.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** One colour stop; [position] is 0..1 along the gradient, or null to space it evenly. */
data class BrushStop(val color: Color, val position: Float? = null)

/**
 * A brush described in the theme JSON, resolved lazily against the size of whatever it paints.
 * All coordinates are fractions of the painted box, so the same theme works for a 40dp icon button
 * and a full-width card.
 */
sealed interface BrushSpec {
    /** `["gradient", c1, c2, ...]` and `["linear", angle, stops...]`: angle in CSS degrees (0 = up, 90 = right, 180 = down). */
    data class Linear(val angleDegrees: Float, val stops: List<BrushStop>) : BrushSpec

    /** `["radial", cx, cy, radius, stops...]`: centre and radius as fractions of the box (radius of the larger side). */
    data class Radial(val centerX: Float, val centerY: Float, val radius: Float, val stops: List<BrushStop>) : BrushSpec

    /** `["sweep", cx, cy, stops...]`: a conic gradient around the centre. */
    data class Sweep(val centerX: Float, val centerY: Float, val stops: List<BrushStop>) : BrushSpec

    /** `["gel", top, bottom]`: Millennium's convex Aqua gel between two colours. */
    data class Gel(val top: Color, val bottom: Color) : BrushSpec
}

/** Fills in missing stop positions by even spacing between known ones; null when none are given. */
internal fun resolveStopPositions(stops: List<BrushStop>): List<Float>? {
    if (stops.none { it.position != null }) return null
    val n = stops.size
    val out = FloatArray(n) { stops[it].position?.coerceIn(0f, 1f) ?: Float.NaN }
    if (out[0].isNaN()) out[0] = 0f
    if (out[n - 1].isNaN()) out[n - 1] = 1f
    var i = 0
    while (i < n) {
        if (out[i].isNaN()) {
            var j = i
            while (out[j].isNaN()) j++
            val a = out[i - 1]
            val b = out[j]
            for (k in i until j) out[k] = a + (b - a) * (k - i + 1) / (j - i + 1)
            i = j
        }
        i++
    }
    for (k in 1 until n) if (out[k] < out[k - 1]) out[k] = out[k - 1]
    return out.toList()
}

internal data class LinearBrush(val angleDegrees: Float, val colors: List<Color>, val positions: List<Float>?) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = angleDegrees * PI / 180.0
        val dx = sin(rad).toFloat()
        val dy = (-cos(rad)).toFloat()
        // CSS rule: the gradient line is long enough that the corners get the end colours exactly.
        val length = abs(size.width * dx) + abs(size.height * dy)
        val c = Offset(size.width / 2f, size.height / 2f)
        val half = Offset(dx * length / 2f, dy * length / 2f)
        return LinearGradientShader(c - half, c + half, colors, positions)
    }
}

internal data class RadialBrush(
    val centerX: Float, val centerY: Float, val radius: Float,
    val colors: List<Color>, val positions: List<Float>?,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader = RadialGradientShader(
        center = Offset(size.width * centerX, size.height * centerY),
        radius = max(1f, max(size.width, size.height) * radius),
        colors = colors,
        colorStops = positions,
    )
}

internal data class SweepBrush(val centerX: Float, val centerY: Float, val colors: List<Color>, val positions: List<Float>?) : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        SweepGradientShader(Offset(size.width * centerX, size.height * centerY), colors, positions)
}
