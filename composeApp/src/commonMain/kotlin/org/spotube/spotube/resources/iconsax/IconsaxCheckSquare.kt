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

val Iconsax.IconsaxCheckSquare: ImageVector
    get() {
        if (_IconsaxCheckSquare != null) {
            return _IconsaxCheckSquare!!
        }
        _IconsaxCheckSquare = ImageVector.Builder(
            name = "IconsaxCheckSquare",
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
                path(fill = SolidColor(Color.White)) {
                    moveTo(16.19f, 2f)
                    horizontalLineTo(7.81f)
                    curveTo(4.17f, 2f, 2f, 4.17f, 2f, 7.81f)
                    verticalLineTo(16.18f)
                    curveTo(2f, 19.83f, 4.17f, 22f, 7.81f, 22f)
                    horizontalLineTo(16.18f)
                    curveTo(19.82f, 22f, 21.99f, 19.83f, 21.99f, 16.19f)
                    verticalLineTo(7.81f)
                    curveTo(22f, 4.17f, 19.83f, 2f, 16.19f, 2f)
                    close()
                    moveTo(16.78f, 9.7f)
                    lineTo(11.11f, 15.37f)
                    curveTo(10.97f, 15.51f, 10.78f, 15.59f, 10.58f, 15.59f)
                    curveTo(10.38f, 15.59f, 10.19f, 15.51f, 10.05f, 15.37f)
                    lineTo(7.22f, 12.54f)
                    curveTo(6.93f, 12.25f, 6.93f, 11.77f, 7.22f, 11.48f)
                    curveTo(7.51f, 11.19f, 7.99f, 11.19f, 8.28f, 11.48f)
                    lineTo(10.58f, 13.78f)
                    lineTo(15.72f, 8.64f)
                    curveTo(16.01f, 8.35f, 16.49f, 8.35f, 16.78f, 8.64f)
                    curveTo(17.07f, 8.93f, 17.07f, 9.4f, 16.78f, 9.7f)
                    close()
                }
            }
        }.build()

        return _IconsaxCheckSquare!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxCheckSquare: ImageVector? = null
