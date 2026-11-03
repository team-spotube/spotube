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

val Iconsax.IconsaxNext: ImageVector
    get() {
        if (_IconsaxNext != null) {
            return _IconsaxNext!!
        }
        _IconsaxNext = ImageVector.Builder(
            name = "IconsaxNext",
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
                    moveTo(3.76f, 7.22f)
                    verticalLineTo(16.79f)
                    curveTo(3.76f, 18.75f, 5.89f, 19.98f, 7.59f, 19f)
                    lineTo(11.74f, 16.61f)
                    lineTo(15.89f, 14.21f)
                    curveTo(17.59f, 13.23f, 17.59f, 10.78f, 15.89f, 9.8f)
                    lineTo(11.74f, 7.4f)
                    lineTo(7.59f, 5.01f)
                    curveTo(5.89f, 4.03f, 3.76f, 5.25f, 3.76f, 7.22f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(20.24f, 18.93f)
                    curveTo(19.83f, 18.93f, 19.49f, 18.59f, 19.49f, 18.18f)
                    verticalLineTo(5.82f)
                    curveTo(19.49f, 5.41f, 19.83f, 5.07f, 20.24f, 5.07f)
                    curveTo(20.65f, 5.07f, 20.99f, 5.41f, 20.99f, 5.82f)
                    verticalLineTo(18.18f)
                    curveTo(20.99f, 18.59f, 20.66f, 18.93f, 20.24f, 18.93f)
                    close()
                }
            }
        }.build()

        return _IconsaxNext!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxNext: ImageVector? = null
