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

val Iconsax.User: ImageVector
    get() {
        if (_User != null) {
            return _User!!
        }
        _User = ImageVector.Builder(
            name = "User",
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
                    moveTo(12f, 12f)
                    curveTo(14.761f, 12f, 17f, 9.761f, 17f, 7f)
                    curveTo(17f, 4.239f, 14.761f, 2f, 12f, 2f)
                    curveTo(9.239f, 2f, 7f, 4.239f, 7f, 7f)
                    curveTo(7f, 9.761f, 9.239f, 12f, 12f, 12f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 14.5f)
                    curveTo(6.99f, 14.5f, 2.91f, 17.86f, 2.91f, 22f)
                    curveTo(2.91f, 22.28f, 3.13f, 22.5f, 3.41f, 22.5f)
                    horizontalLineTo(20.59f)
                    curveTo(20.87f, 22.5f, 21.09f, 22.28f, 21.09f, 22f)
                    curveTo(21.09f, 17.86f, 17.01f, 14.5f, 12f, 14.5f)
                    close()
                }
            }
        }.build()

        return _User!!
    }

@Suppress("ObjectPropertyName")
private var _User: ImageVector? = null
