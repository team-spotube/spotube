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

val Iconsax.IconsaxPlay: ImageVector
    get() {
        if (_IconsaxPlay != null) {
            return _IconsaxPlay!!
        }
        _IconsaxPlay = ImageVector.Builder(
            name = "IconsaxPlay",
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
                    moveTo(18.7f, 8.98f)
                    lineTo(4.14f, 17.71f)
                    curveTo(4.05f, 17.38f, 4f, 17.03f, 4f, 16.67f)
                    verticalLineTo(7.33f)
                    curveTo(4f, 4.25f, 7.33f, 2.33f, 10f, 3.87f)
                    lineTo(14.04f, 6.2f)
                    lineTo(18.09f, 8.54f)
                    curveTo(18.31f, 8.67f, 18.52f, 8.81f, 18.7f, 8.98f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(18.09f, 15.46f)
                    lineTo(14.04f, 17.8f)
                    lineTo(10f, 20.13f)
                    curveTo(8.09f, 21.23f, 5.84f, 20.57f, 4.72f, 18.96f)
                    lineTo(5.14f, 18.71f)
                    lineTo(19.58f, 10.05f)
                    curveTo(20.58f, 11.85f, 20.09f, 14.31f, 18.09f, 15.46f)
                    close()
                }
            }
        }.build()

        return _IconsaxPlay!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxPlay: ImageVector? = null
