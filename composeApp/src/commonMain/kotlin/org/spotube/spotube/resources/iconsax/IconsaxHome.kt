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

val Iconsax.IconsaxHome: ImageVector
    get() {
        if (_IconsaxHome != null) {
            return _IconsaxHome!!
        }
        _IconsaxHome = ImageVector.Builder(
            name = "IconsaxHome",
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
                    moveTo(20.83f, 8.01f)
                    lineTo(14.28f, 2.77f)
                    curveTo(13f, 1.75f, 11f, 1.74f, 9.73f, 2.76f)
                    lineTo(3.18f, 8.01f)
                    curveTo(2.24f, 8.76f, 1.67f, 10.26f, 1.87f, 11.44f)
                    lineTo(3.13f, 18.98f)
                    curveTo(3.42f, 20.67f, 4.99f, 22f, 6.7f, 22f)
                    horizontalLineTo(17.3f)
                    curveTo(18.99f, 22f, 20.59f, 20.64f, 20.88f, 18.97f)
                    lineTo(22.14f, 11.43f)
                    curveTo(22.32f, 10.26f, 21.75f, 8.76f, 20.83f, 8.01f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 18.75f)
                    curveTo(11.59f, 18.75f, 11.25f, 18.41f, 11.25f, 18f)
                    verticalLineTo(15f)
                    curveTo(11.25f, 14.59f, 11.59f, 14.25f, 12f, 14.25f)
                    curveTo(12.41f, 14.25f, 12.75f, 14.59f, 12.75f, 15f)
                    verticalLineTo(18f)
                    curveTo(12.75f, 18.41f, 12.41f, 18.75f, 12f, 18.75f)
                    close()
                }
            }
        }.build()

        return _IconsaxHome!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxHome: ImageVector? = null
