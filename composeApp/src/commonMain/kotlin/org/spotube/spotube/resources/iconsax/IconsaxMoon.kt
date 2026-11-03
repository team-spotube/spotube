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

val Iconsax.IconsaxMoon: ImageVector
    get() {
        if (_IconsaxMoon != null) {
            return _IconsaxMoon!!
        }
        _IconsaxMoon = ImageVector.Builder(
            name = "IconsaxMoon",
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
                    moveTo(9f, 19f)
                    curveTo(9f, 19.84f, 9.13f, 20.66f, 9.37f, 21.42f)
                    curveTo(5.53f, 20.09f, 2.63f, 16.56f, 2.33f, 12.43f)
                    curveTo(2.03f, 8.04f, 4.56f, 3.94f, 8.65f, 2.22f)
                    curveTo(9.71f, 1.78f, 10.25f, 2.1f, 10.48f, 2.33f)
                    curveTo(10.7f, 2.55f, 11.01f, 3.08f, 10.57f, 4.09f)
                    curveTo(10.12f, 5.13f, 9.9f, 6.23f, 9.9f, 7.37f)
                    curveTo(9.91f, 9.41f, 10.71f, 11.3f, 12.01f, 12.75f)
                    curveTo(10.18f, 14.21f, 9f, 16.47f, 9f, 19f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(21.21f, 17.72f)
                    curveTo(19.23f, 20.41f, 16.09f, 21.99f, 12.74f, 21.99f)
                    curveTo(12.58f, 21.99f, 12.42f, 21.98f, 12.26f, 21.97f)
                    curveTo(11.26f, 21.93f, 10.29f, 21.74f, 9.37f, 21.42f)
                    curveTo(9.13f, 20.66f, 9f, 19.84f, 9f, 19f)
                    curveTo(9f, 16.47f, 10.18f, 14.21f, 12.01f, 12.75f)
                    curveTo(13.48f, 14.4f, 15.59f, 15.47f, 17.92f, 15.57f)
                    curveTo(18.55f, 15.6f, 19.18f, 15.55f, 19.8f, 15.44f)
                    curveTo(20.92f, 15.24f, 21.37f, 15.66f, 21.53f, 15.93f)
                    curveTo(21.7f, 16.2f, 21.88f, 16.79f, 21.21f, 17.72f)
                    close()
                }
            }
        }.build()

        return _IconsaxMoon!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMoon: ImageVector? = null
