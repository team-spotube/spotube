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

val Iconsax.IconsaxMusic: ImageVector
    get() {
        if (_IconsaxMusic != null) {
            return _IconsaxMusic!!
        }
        _IconsaxMusic = ImageVector.Builder(
            name = "IconsaxMusic",
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
                    moveTo(10.29f, 10.34f)
                    verticalLineTo(18.41f)
                    curveTo(10.29f, 20.39f, 8.67f, 22f, 6.7f, 22f)
                    curveTo(4.72f, 22f, 3.11f, 20.39f, 3.11f, 18.41f)
                    curveTo(3.11f, 16.44f, 4.72f, 14.83f, 6.7f, 14.83f)
                    curveTo(7.53f, 14.83f, 8.28f, 15.12f, 8.89f, 15.59f)
                    verticalLineTo(10.74f)
                    lineTo(10.29f, 10.34f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(20.89f, 7.32f)
                    verticalLineTo(16.48f)
                    curveTo(20.89f, 18.46f, 19.28f, 20.07f, 17.3f, 20.07f)
                    curveTo(15.33f, 20.07f, 13.71f, 18.46f, 13.71f, 16.48f)
                    curveTo(13.71f, 14.51f, 15.33f, 12.9f, 17.3f, 12.9f)
                    curveTo(18.14f, 12.9f, 18.89f, 13.19f, 19.5f, 13.67f)
                    verticalLineTo(7.72f)
                    lineTo(20.89f, 7.32f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(20.89f, 5.18f)
                    verticalLineTo(7.32f)
                    lineTo(8.89f, 10.74f)
                    verticalLineTo(6.75f)
                    curveTo(8.89f, 5.28f, 9.78f, 4.14f, 11.19f, 3.76f)
                    lineTo(16.97f, 2.18f)
                    curveTo(18.14f, 1.86f, 19.13f, 1.97f, 19.83f, 2.51f)
                    curveTo(20.54f, 3.04f, 20.89f, 3.94f, 20.89f, 5.18f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusic!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusic: ImageVector? = null
