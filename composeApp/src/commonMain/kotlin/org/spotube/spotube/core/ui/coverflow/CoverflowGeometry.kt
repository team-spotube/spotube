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

package org.spotube.spotube.core.ui.coverflow

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.min

@Stable
internal class Geometry(
    internal val params: CoverflowParams = CoverflowParams(),
    private val size: IntSize = IntSize.Zero) {
    @Stable
    private val shortEdge
        get() = min(size.width, size.height)

    @Stable
    private val containerCenter
        get() = size.width / 2

    @Stable
    private val zoomDelta
        get() = 1 - params.zoom

    @Stable
    internal val coverSize
        get() = shortEdge * params.size

    @Stable
    internal val coverOffset
        get() = (coverSize * params.offset).toInt()

    @Stable
    internal val spacerWidth
        get() = containerCenter - coverOffset / 2

    @Stable
    internal fun isSelected(horizontalPosition: Float): Boolean {
        return distanceToCenter(horizontalPosition).toInt() == 0
    }

    @Stable
    internal fun distanceToCenter(horizontalPosition: Float): Float {
        return horizontalPosition + coverOffset * 0.5f - containerCenter
    }

    @Stable
    internal fun effectFactor(distanceToCenter: Float): Float {
        val relative = distanceToCenter / coverSize
        val absolute = abs(relative)
        val start = 0f
        val end = 0.5f
        var factor = if (absolute <= start) {
            0f
        } else if (absolute >= end) {
            1f
        } else {
            val intervalStep = absolute - start
            val delta = end - start
            intervalStep / delta
        }
        if (relative < 0) {
            factor = -factor
        }
        return factor
    }

    @Stable
    internal fun rotation(distanceToCenter: Float): Float {
        return -params.angle * effectFactor(distanceToCenter)
    }

    @Stable
    internal fun scale(distanceToCenter: Float): Float {
        return 1 - zoomDelta * abs(effectFactor(distanceToCenter))
    }

    @Stable
    internal fun translationX(distanceToCenter: Float): Float {
        return coverOffset * params.shift * effectFactor(distanceToCenter)
    }
}
