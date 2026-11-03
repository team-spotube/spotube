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

package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.InconsaxClock: ImageVector
    get() {
        if (_InconsaxClock != null) {
            return _InconsaxClock!!
        }
        _InconsaxClock = ImageVector.Builder(
            name = "InconsaxClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(12f, 22f)
                    curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                    curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                    curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                    curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.71f, 15.93f)
                    curveTo(15.58f, 15.93f, 15.45f, 15.9f, 15.33f, 15.82f)
                    lineTo(12.23f, 13.97f)
                    curveTo(11.46f, 13.51f, 10.89f, 12.5f, 10.89f, 11.61f)
                    verticalLineTo(7.51f)
                    curveTo(10.89f, 7.1f, 11.23f, 6.76f, 11.64f, 6.76f)
                    curveTo(12.05f, 6.76f, 12.39f, 7.1f, 12.39f, 7.51f)
                    verticalLineTo(11.61f)
                    curveTo(12.39f, 11.97f, 12.69f, 12.5f, 13f, 12.68f)
                    lineTo(16.1f, 14.53f)
                    curveTo(16.46f, 14.74f, 16.57f, 15.2f, 16.36f, 15.56f)
                    curveTo(16.21f, 15.8f, 15.96f, 15.93f, 15.71f, 15.93f)
                    close()
                }
            }
        }.build()

        return _InconsaxClock!!
    }

@Suppress("ObjectPropertyName")
private var _InconsaxClock: ImageVector? = null
